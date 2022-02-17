package com.rarible.flow.api.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.cadence.JsonCadenceBuilder
import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.nftco.flow.sdk.cadence.NumberField
import com.nftco.flow.sdk.cadence.OptionalField
import com.nftco.flow.sdk.cadence.UInt32NumberField
import com.rarible.flow.Contracts
import com.rarible.flow.api.metaprovider.CnnNFTConverter
import com.rarible.flow.api.metaprovider.DisruptArtNFT
import com.rarible.flow.api.metaprovider.RaribleNFT
import com.rarible.flow.api.service.flowrpc.ScanUserNftScript
import com.rarible.flow.api.service.flowrpc.TopShotMomentScript
import com.rarible.flow.core.config.AppProperties
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.Part
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.events.RaribleNFTv2Token
import com.rarible.flow.pmap
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.time.Instant


@Service
class UserStorageService(
    private val scriptExecutor: ScriptExecutor,
    private val itemRepository: ItemRepository,
    private val appProperties: AppProperties,
    private val protocolEventPublisher: ProtocolEventPublisher,
    private val ownershipRepository: OwnershipRepository,
    private val scanUserNftScript: ScanUserNftScript,
    private val topShotMomentScript: TopShotMomentScript
) {

    private val log: Logger = LoggerFactory.getLogger(UserStorageService::class.java)

    suspend fun scanNFT(address: FlowAddress) {
        log.info("Scan user NFT's for address ${address.formatted}")
        val objectMapper = jacksonObjectMapper()
        val parser = JsonCadenceParser()
        val builder = JsonCadenceBuilder()
        scanUserNftScript.call(address).forEach { (collection, itemIds) ->
            try {
                processItem(collection, itemIds, objectMapper,builder, parser, address)
            } catch (e: Exception) {
                log.error("Failed to save [{}] ids: {}", collection, itemIds, e)
                emptyList<Item>()
            }
        }
        log.info("Scan NFT's for address [${address.formatted}] complete!")
    }


    private suspend fun processItem(
        collection: String,
        itemIds: List<Long>,
        objectMapper: ObjectMapper,
        builder: JsonCadenceBuilder,
        parser: JsonCadenceParser,
        address: FlowAddress
    ) {
        when (collection) {
            "TopShot" -> {
                itemIds.forEach {
                    val contract = contract("0xTOPSHOTTOKEN", "TopShot")
                    val topShotContractAddress = contractAddress("0xTOPSHOTTOKEN")
                    if (notExistsItem(contract, it)) {
                        val momentData = topShotMomentScript.call(address, it)
                        val item = Item(
                            contract = contract,
                            tokenId = it,
                            creator = topShotContractAddress,
                            royalties = listOf(
                                Part(
                                    address = topShotContractAddress,
                                    fee = 0.05
                                )
                            ),
                            owner = address,
                            mintedAt = Instant.now(),
                            meta = objectMapper.writeValueAsString(momentData),
                            collection = contract,
                            updatedAt = Instant.now()
                        )
                        saveItem(item)
                    } else {
                        val item =
                            itemRepository.findById(ItemId(contract, it)).awaitSingle()
                        saveItem(
                            item.copy(
                                owner = address,
                                royalties = listOf(
                                    Part(
                                        address = topShotContractAddress,
                                        fee = 0.05
                                    )
                                ),
                                updatedAt = Instant.now()
                            )
                        )
                    }
                }
            }
            Contracts.MOTOGP.contractName -> {
                val contract = Contracts.MOTOGP.fqn(appProperties.chainId)
                val items = itemRepository.findAllByIdIn(
                    itemIds.map { ItemId(contract, it) }.toSet()
                ).toIterable().associateBy { it.tokenId }
                itemIds.pmap { tokenId ->
                    val item = items[tokenId]
                    if (item == null) {
                        Item(
                            contract = contract,
                            tokenId = tokenId,
                            creator = Contracts.MOTOGP.deployments[appProperties.chainId]!!,
                            owner = address,
                            royalties = Contracts.MOTOGP.staticRoyalties(appProperties.chainId),
                            mintedAt = Instant.now(),
                            meta = "{}",
                            collection = contract,
                            updatedAt = Instant.now()
                        )
                    } else {
                        if (item.owner != address) {
                            item.copy(owner = address, updatedAt = Instant.now())
                        } else {
                            checkOwnership(item, address)
                            null
                        }
                    }
                }
            }
            Contracts.EVOLUTION.contractName -> {
                val contract = contract(Contracts.EVOLUTION.import, Contracts.EVOLUTION.contractName)
                val items = itemRepository.findAllByIdIn(
                    itemIds.map { ItemId(contract, it) }.toSet()
                ).toIterable().associateBy { it.tokenId }
                itemIds.pmap { tokenId ->
                    val item = items[tokenId]
                    if (item == null) {
                        val initialMeta = scriptExecutor.executeFile(
                            "/script/get_evolution_nft.cdc",
                            {
                                arg { address(address.bytes) }
                                arg { uint64(tokenId) }
                            }, {
                                dictionaryMap(it) { k, v ->
                                    string(k) to int(v)
                                }
                            }
                        )
                        Item(
                            contract = contract,
                            tokenId = tokenId,
                            creator = contractAddress(Contracts.EVOLUTION.import),
                            owner = address,
                            royalties = Contracts.EVOLUTION.staticRoyalties(appProperties.chainId),
                            mintedAt = Instant.now(),
                            meta = objectMapper.writeValueAsString(initialMeta),
                            collection = contract,
                            updatedAt = Instant.now()
                        )
                    } else {
                        if (item.owner != address) {
                            item.copy(owner = address, updatedAt = Instant.now())
                        } else {
                            checkOwnership(item, address)
                            null
                        }
                    }
                }
            }
            "RaribleNFT" -> {
                val contract = contract("0xRARIBLETOKEN", "RaribleNFT")
                val items = itemRepository.findAllByIdIn(
                    itemIds.map { ItemId(contract, it) }.toSet()
                ).toIterable().associateBy { it.tokenId }
                itemIds.pmap { tokenId ->
                    val item = items[tokenId]
                    if (item == null) {
                        val token = scriptExecutor.executeFile(
                            "/script/get_rarible_nft.cdc",
                            {
                                arg { address(address.bytes) }
                                arg { uint64(tokenId) }
                            }, {
                                optional(it) { json ->
                                    unmarshall<RaribleNFT>(json)
                                }!!
                            }
                        )
                        Item(
                            contract = contract,
                            tokenId = tokenId,
                            creator = token.creator,
                            royalties = token.royalties,
                            owner = address,
                            mintedAt = Instant.now(),
                            meta = objectMapper.writeValueAsString(token.metadata),
                            collection = contract,
                            updatedAt = Instant.now()
                        )
                    } else {
                        if (item.owner != address) {
                            item.copy(owner = address, updatedAt = Instant.now())
                        } else {
                            checkOwnership(item, address)
                            null
                        }
                    }
                }
            }
            "MugenNFT" -> {
                val contract = contract("0xMUGENNFT", "MugenNFT")
                val items = itemRepository.findAllByIdIn(
                    itemIds.map { ItemId(contract, it) }.toSet()
                ).toIterable().associateBy { it.tokenId }
                itemIds.pmap { tokenId ->
                    val item = items[tokenId]
                    if (item == null) {
                        Item(
                            contract = contract,
                            tokenId = tokenId,
                            creator = contractAddress("0xMUGENNFT"),
                            royalties = emptyList(),
                            owner = address,
                            mintedAt = Instant.now(),
                            meta = "{}",
                            collection = contract,
                            updatedAt = Instant.now()
                        )
                    } else {
                        if (item.owner != address) {
                            item.copy(owner = address, updatedAt = Instant.now())
                        } else {
                            checkOwnership(item, address)
                            null
                        }
                    }
                }
            }

            Contracts.MATRIX_WORLD_VOUCHER.contractName -> {
                val contract = contract(Contracts.MATRIX_WORLD_VOUCHER.import, Contracts.MATRIX_WORLD_VOUCHER.contractName)
                val items = itemRepository.findAllByIdIn(
                    itemIds.map { ItemId(contract, it) }.toSet()
                ).toIterable().associateBy { it.tokenId }
                itemIds.pmap { tokenId ->
                    val item = items[tokenId]
                    if (item == null) {
                        Item(
                            contract = contract,
                            tokenId = tokenId,
                            creator = contractAddress(Contracts.MATRIX_WORLD_VOUCHER.import),
                            royalties = Contracts.MATRIX_WORLD_VOUCHER.staticRoyalties(appProperties.chainId),
                            owner = address,
                            mintedAt = Instant.now(),
                            meta = "{}",
                            collection = contract,
                            updatedAt = Instant.now()
                        )
                    } else {
                        if (item.owner != address) {
                            item.copy(owner = address, updatedAt = Instant.now())
                        } else {
                            checkOwnership(item, address)
                            null
                        }
                    }
                }
            }

            Contracts.MATRIX_WORLD_FLOW_FEST.contractName -> {
                itemIds.forEach { tokenId ->
                    val contract = contract(Contracts.MATRIX_WORLD_FLOW_FEST.import, Contracts.MATRIX_WORLD_FLOW_FEST.contractName)
                    val item = if (notExistsItem(contract, tokenId)) {
                        Item(
                            contract = contract,
                            tokenId = tokenId,
                            creator = contractAddress(Contracts.MATRIX_WORLD_FLOW_FEST.import),
                            royalties = Contracts.MATRIX_WORLD_FLOW_FEST.staticRoyalties(appProperties.chainId),
                            owner = address,
                            mintedAt = Instant.now(),
                            meta = "{}",
                            collection = contract,
                            updatedAt = Instant.now()
                        )
                    } else {
                        val i = itemRepository.findById(ItemId(contract, tokenId)).awaitSingle()
                        if (i.owner != address) {
                            i.copy(owner = address, updatedAt = Instant.now())
                        } else {
                            checkOwnership(i, address)
                            null
                        }
                    }
                    saveItem(item)
                }
            }

            Contracts.CNN.contractName -> {
                val contract = Contracts.CNN.fqn(appProperties.chainId)
                itemIds.pmap { tokenId ->
                    val item = if (notExistsItem(contract, tokenId)) {
                        val tokenData =
                            scriptExecutor.executeFile(
                                "/script/get_cnn_nft.cdc",
                                {
                                    arg { address(address.bytes) }
                                    arg { uint64(tokenId) }
                                },
                                {
                                    CnnNFTConverter.convert(it as OptionalField)
                                }
                            )

                        Item(
                            contract = contract,
                            tokenId = tokenId,
                            creator = contractAddress(Contracts.CNN.import),
                            royalties = Contracts.CNN.staticRoyalties(appProperties.chainId),
                            owner = address,
                            mintedAt = Instant.now(),
                            meta = objectMapper.writeValueAsString(tokenData),
                            collection = contract,
                            updatedAt = Instant.now()
                        )
                    } else {
                        val i = itemRepository.findById(ItemId(contract, tokenId)).awaitSingle()
                        if (i.owner != address) {
                            i.copy(owner = address, updatedAt = Instant.now())
                        } else {
                            checkOwnership(i, address)
                            null
                        }
                    }
                    saveItem(item)
                }
            }
            "DisruptArt" -> {
                itemIds.forEach { tokenId ->
                    val contract = contract("0xDISRUPTART", "DisruptArt")
                    val item = if (notExistsItem(contract, tokenId)) {
                        val tokenData = Flow.unmarshall(
                            DisruptArtNFT::class,
                            scriptExecutor.execute(
                                code = scriptText("/script/disrupt_art_nft.cdc"),
                                args = mutableListOf(
                                    builder.address(address.bytes),
                                    builder.uint64(tokenId)
                                )
                            ).jsonCadence
                        )

                        Item(
                            contract = contract,
                            tokenId = tokenId,
                            creator = FlowAddress(tokenData.creator),
                            royalties = listOf(
                                Part(
                                    address = contractAddress("0xDISRUPTARTROYALTY"),
                                    fee = 0.15
                                )
                            ),
                            collection = contract,
                            mintedAt = Instant.now(),
                            updatedAt = Instant.now(),
                            owner = address,
                            meta = objectMapper.writeValueAsString(tokenData.metaData)
                        )
                    } else {
                        val i = itemRepository.findById(ItemId(contract, tokenId)).awaitSingle()
                        if (i.owner != address) {
                            i.copy(owner = address, updatedAt = Instant.now())
                        } else {
                            checkOwnership(i, address)
                            null
                        }
                    }
                    saveItem(item)
                }
            }
            "RaribleNFTv2" -> {
                scriptExecutor.executeFile(
                    "/script/get_rari_v2_items.cdc",
                    {
                        arg { address(address.bytes) }
                    },
                    { tokens ->
                        arrayValues(tokens) {
                            unmarshall(it, RaribleNFTv2Token::class)
                        }.map { token ->
                            Item(
                                contract = contract("0xRARIBLENFT_V2", "RaribleNFTv2"),
                                tokenId = token.id,
                                creator = FlowAddress(token.creator),
                                owner = address,
                                mintedAt = Instant.now(),
                                royalties = token.royalties.map { Part(address = FlowAddress(it.address), fee = it.fee.toDouble()) },
                                updatedAt = Instant.now(),
                                collection = "${contract("0xSOFTCOLLECTION", "SoftCollection")}:${token.parentId}",
                                meta = objectMapper.writeValueAsString(token.meta.toMap())
                            )
                        }
                    }
                )
            }

            Contracts.CHAINMONSTERS.contractName -> {
                itemIds.forEach { tokenId ->
                    val contract = Contracts.CHAINMONSTERS.fqn(appProperties.chainId)
                    val item = if (notExistsItem(contract, tokenId)) {
                        val rewardId: Int? = scriptExecutor.executeFile(
                            "/script/item/chainmonsters.cdc",
                            {
                                arg { address(address) }
                                arg { uint64(tokenId) }
                            },
                            { json ->
                                json.value?.let { v ->
                                    v as UInt32NumberField
                                    v.toInt()
                                }
                            }
                        )

                        Item(
                            contract = contract,
                            tokenId = tokenId,
                            creator = Contracts.CHAINMONSTERS.deployments[appProperties.chainId]!!,
                            royalties = Contracts.CHAINMONSTERS.staticRoyalties(appProperties.chainId),
                            owner = address,
                            mintedAt = Instant.now(),
                            meta = "{\"rewardId\": \"$rewardId\"}",
                            collection = contract,
                            updatedAt = Instant.now()
                        )
                    } else {
                        val i = itemRepository.findById(ItemId(contract, tokenId)).awaitSingle()
                        if (i.owner != address) {
                            i.copy(owner = address, updatedAt = Instant.now())
                        } else {
                            checkOwnership(i, address)
                            null
                        }
                    }
                    saveItem(item)
                }
            }

            Contracts.ONE_FOOTBALL.contractName -> {
                itemIds.forEach { tokenId ->
                    val contract = Contracts.ONE_FOOTBALL.fqn(appProperties.chainId)
                    val item = if (notExistsItem(contract, tokenId)) {
                        Item(
                            contract = contract,
                            tokenId = tokenId,
                            creator = Contracts.ONE_FOOTBALL.deployments[appProperties.chainId]!!,
                            royalties = Contracts.ONE_FOOTBALL.staticRoyalties(appProperties.chainId),
                            owner = address,
                            mintedAt = Instant.now(),
                            meta = "{}",
                            collection = contract,
                            updatedAt = Instant.now()
                        )
                    } else {
                        val i = itemRepository.findById(ItemId(contract, tokenId)).awaitSingle()
                        if (i.owner != address) {
                            i.copy(owner = address, updatedAt = Instant.now())
                        } else {
                            checkOwnership(i, address)
                            null
                        }
                    }
                    saveItem(item)
                }
            }

            Contracts.JAMBB_MOMENTS.contractName -> {
                itemIds.forEach { tokenId ->
                    val contract = Contracts.JAMBB_MOMENTS.fqn(appProperties.chainId)
                    val item = if (notExistsItem(contract, tokenId)) {
                        Item(
                            contract = contract,
                            tokenId = tokenId,
                            creator = Contracts.JAMBB_MOMENTS.deployments[appProperties.chainId]!!,
                            royalties = Contracts.JAMBB_MOMENTS.staticRoyalties(appProperties.chainId),
                            owner = address,
                            mintedAt = Instant.now(),
                            meta = "{}",
                            collection = contract,
                            updatedAt = Instant.now()
                        )
                    } else {
                        val i = itemRepository.findById(ItemId(contract, tokenId)).awaitSingle()
                        if (i.owner != address) {
                            i.copy(owner = address, updatedAt = Instant.now())
                        } else {
                            checkOwnership(i, address)
                            null
                        }
                    }
                    saveItem(item)
                }
            }

            Contracts.FANFARE.contractName -> {
                itemIds.forEach { tokenId ->
                    val contract = Contracts.FANFARE.fqn(appProperties.chainId)
                    val item = if (notExistsItem(contract, tokenId)) {
                        val meta: String? = scriptExecutor.executeFile(
                            "/script/item/item_fanfare.cdc",
                            {
                                arg { address(address.formatted) }
                                arg { uint64(tokenId) }
                            }, { json ->
                                json.value as String?
                            }
                        )
                        Item(
                            contract = contract,
                            tokenId = tokenId,
                            creator = Contracts.FANFARE.deployments[appProperties.chainId]!!,
                            royalties = Contracts.FANFARE.staticRoyalties(appProperties.chainId),
                            owner = address,
                            mintedAt = Instant.now(),
                            meta = objectMapper.writeValueAsString(mapOf(
                                "metadata" to meta
                            )),
                            collection = contract,
                            updatedAt = Instant.now()
                        )
                    } else {
                        val i = itemRepository.findById(ItemId(contract, tokenId)).awaitSingle()
                        if (i.owner != address) {
                            i.copy(owner = address, updatedAt = Instant.now())
                        } else {
                            checkOwnership(i, address)
                            null
                        }
                    }
                    saveItem(item)
                }
            }
        }
    }

    private fun contractAddress(alias: String): FlowAddress {
        return Flow.DEFAULT_ADDRESS_REGISTRY.addressOf(
            alias,
            appProperties.chainId
        )!!
    }

    private fun contract(tokenAlias: String, contractName: String): String {
        val address = Flow.DEFAULT_ADDRESS_REGISTRY.addressOf(tokenAlias, appProperties.chainId)!!
        return "A.${address.base16Value}.$contractName"
    }

    private suspend fun notExistsItem(contract: String, tokenId: TokenId): Boolean {
        return !itemRepository.existsById(
            ItemId(
                contract = contract,
                tokenId = tokenId
            )
        ).awaitSingle()
    }

    private suspend fun saveItem(item: Item?) {
        log.debug("saveItem: $item")
        if (item != null) {
            val a = itemRepository.save(item).awaitSingle()
            protocolEventPublisher.onItemUpdate(a)
            checkOwnership(item, item.owner!!)
        }
    }

    private suspend fun checkOwnership(item: Item, to: FlowAddress) {
        ownershipRepository.deleteAllByContractAndTokenIdAndOwnerNot(item.contract, item.tokenId, to).asFlow().toList()
            .forEach { protocolEventPublisher.onDelete(it) }
        val o = ownershipRepository.findById(item.ownershipId(to)).awaitSingleOrNull()
            ?: Ownership(item.ownershipId(to), item.creator)
        protocolEventPublisher.onUpdate(ownershipRepository.save(o).awaitSingle())
    }

    private fun scriptText(resourcePath: String): String {
        val resource = ClassPathResource(resourcePath)
        return resource.inputStream.bufferedReader().use { it.readText() }
    }

}


