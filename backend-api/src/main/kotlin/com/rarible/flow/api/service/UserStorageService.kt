package com.rarible.flow.api.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.cadence.JsonCadenceBuilder
import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.rarible.flow.api.metaprovider.CnnNFTConverter
import com.rarible.flow.api.metaprovider.RaribleNFT
import com.rarible.flow.core.config.AppProperties
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.Part
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OwnershipRepository
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
) {

    private val log: Logger = LoggerFactory.getLogger(UserStorageService::class.java)

    suspend fun scanNFT(address: FlowAddress) {
        log.info("Scan user NFT's for address ${address.formatted}")
        val builder = JsonCadenceBuilder()
        val response =
            scriptExecutor.execute(scriptText("/script/all_nft_ids.cdc"), mutableListOf(builder.address(address.bytes)))
        log.info(response.stringValue)

        val parser = JsonCadenceParser()
        val data = parser.dictionaryMap(response.jsonCadence) { k, v ->
            string(k) to arrayValues(v) {
                long(it)
            }
        }
        log.info("$data")
        val objectMapper = ObjectMapper()

        data.forEach { (collection, itemIds) ->
            try {
                processItems(collection, itemIds, objectMapper, builder, parser, address).forEach { item ->
                    saveItem(item)
                }
            } catch (e: Exception) {
                log.error("Failed to save [{}] ids: {}", collection, itemIds, e)
            }
        }
        log.info("Scan NFT's for address [${address.formatted}] complete!")
    }

    private suspend fun processItems(
        collection: String,
        itemIds: List<Long>,
        objectMapper: ObjectMapper,
        builder: JsonCadenceBuilder,
        parser: JsonCadenceParser,
        address: FlowAddress
    ): List<Item?> {
        return when (collection) {
            "TopShot" -> {
                val contract = contract("0xTOPSHOTTOKEN", "TopShot")
                val items = itemRepository.findAllByIdIn(
                    itemIds.map { ItemId(contract, it) }.toSet()
                ).toIterable().associateBy { it.tokenId }
                itemIds.map { tokenId ->
                    val item = items[tokenId]
                    if(item == null) {
                        val res = scriptExecutor.execute(
                            scriptText("/script/get_topshot_moment.cdc"),
                            mutableListOf(builder.address(address.bytes), builder.uint64(tokenId))
                        )
                        val momentData = parser.dictionaryMap(res.jsonCadence) { k, v ->
                            string(k) to long(v)
                        }
                        Item(
                            contract = contract,
                            tokenId = tokenId,
                            creator = contractAddress("0xTOPSHOTTOKEN"),
                            royalties = listOf(
                                Part(
                                    address = contractAddress("0xTOPSHOTTOKEN"),
                                    fee = 0.05
                                )
                            ),
                            owner = address,
                            mintedAt = Instant.now(),
                            meta = objectMapper.writeValueAsString(momentData),
                            collection = contract,
                            updatedAt = Instant.now()
                        )
                    } else {
                        item.copy(
                            owner = address,
                            royalties = listOf(
                                Part(
                                    address = contractAddress("0xTOPSHOTTOKEN"),
                                    fee = 0.05
                                )
                            ),
                            updatedAt = Instant.now()
                        )
                    }
                }
            }
            "MotoGPCard" -> {
                val contract = contract("0xMOTOGPTOKEN", "MotoGPCard")
                val items = itemRepository.findAllByIdIn(
                    itemIds.map { ItemId(contract, it) }.toSet()
                ).toIterable().associateBy { it.tokenId }
                itemIds.map { tokenId ->
                    val item = items[tokenId]
                    if(item == null) {
                        Item(
                            contract = contract,
                            tokenId = tokenId,
                            creator = contractAddress("0xMOTOGPTOKEN"),
                            owner = address,
                            royalties = emptyList(),
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
            "Evolution" -> {
                val contract = contract("0xEVOLUTIONTOKEN", "Evolution")
                val items = itemRepository.findAllByIdIn(
                    itemIds.map { ItemId(contract, it) }.toSet()
                ).toIterable().associateBy { it.tokenId }
                itemIds.map { tokenId ->
                    val item = items[tokenId]
                    if (item == null) {
                        val res = scriptExecutor.execute(
                            scriptText("/script/get_evolution_nft.cdc"),
                            mutableListOf(builder.address(address.bytes), builder.uint64(tokenId))
                        )
                        val initialMeta = parser.dictionaryMap(res.jsonCadence) { k, v ->
                            string(k) to int(v)
                        }
                        Item(
                            contract = contract,
                            tokenId = tokenId,
                            creator = contractAddress("0xEVOLUTIONTOKEN"),
                            owner = address,
                            royalties = emptyList(),
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
                itemIds.map { tokenId ->
                    val item = items[tokenId]
                    if (item == null) {
                        val res = scriptExecutor.execute(
                            scriptText("/script/get_rarible_nft.cdc"), mutableListOf(
                                builder.address(address.bytes),
                                builder.uint64(tokenId)
                            )
                        )

                        val token = parser.optional(res.jsonCadence) {
                            unmarshall<RaribleNFT>(it)
                        }!!
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
                itemIds.map { tokenId ->
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

            "MatrixWorldVoucher" -> {
                val contract = contract("0xMATRIXWORLD", "MatrixWorldVoucher")
                val items = itemRepository.findAllByIdIn(
                    itemIds.map { ItemId(contract, it) }.toSet()
                ).toIterable().associateBy { it.tokenId }
                itemIds.map { tokenId ->
                    val item = items[tokenId]
                    if (item == null) {
                        Item(
                            contract = contract,
                            tokenId = tokenId,
                            creator = contractAddress("0xMATRIXWORLD"),
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

            "MatrixWorldFlowFestNFT" -> {
                itemIds.forEach { tokenId ->
                    val contract = contract("0xMATRIXWORLDFLOWFEST", "MatrixWorldFlowFestNFT")
                    val item = if (notExistsItem(contract, tokenId)) {
                        Item(
                            contract = contract,
                            tokenId = tokenId,
                            creator = contractAddress("0xMATRIXWORLDFLOWFEST"),
                            royalties = emptyList(),
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

            "CNN_NFT" -> {
                val contract = contract("0xCNNNFT", "CNN_NFT")
                val items = itemRepository.findAllByIdIn(
                    itemIds.map { ItemId(contract, it) }.toSet()
                ).toIterable().associateBy { it.tokenId }
                itemIds.map { tokenId ->
                    val item = items[tokenId]
                    if (item == null) {
                        val tokenData = CnnNFTConverter.convert(
                            scriptExecutor.execute(
                                scriptText("/script/get_cnn_nft.cdc"), mutableListOf(
                                    builder.address(address.bytes),
                                    builder.uint64(tokenId)
                                )
                            )
                        )

                        Item(
                            contract = contract,
                            tokenId = tokenId,
                            creator = contractAddress("0xCNNNFT"),
                            royalties = emptyList(),
                            owner = address,
                            mintedAt = Instant.now(),
                            meta = objectMapper.writeValueAsString(tokenData),
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
            else -> emptyList()
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
