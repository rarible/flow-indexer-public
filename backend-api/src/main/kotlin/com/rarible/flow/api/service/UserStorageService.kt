package com.rarible.flow.api.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.cadence.OptionalField
import com.rarible.flow.api.metaprovider.CnnNFTConverter
import com.rarible.flow.api.metaprovider.RaribleNFT
import com.rarible.flow.core.config.AppProperties
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.Part
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.pflatMap
import com.rarible.flow.pmap
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
        val data = scriptExecutor.executeFile(
            "/script/all_nft_ids.cdc",
            {
                arg { address(address.bytes) }
            },
            { json ->
                dictionaryMap(json) { k, v ->
                    string(k) to arrayValues(v) { field ->
                        long(field)
                    }
                }
            }
        )

        log.info("User {} NFTs: {}", address.formatted, data)
        val objectMapper = ObjectMapper()

        data.pflatMap { (collection, itemIds) ->
            try {
                processItems(collection, itemIds, objectMapper, address)
            } catch (e: Exception) {
                log.error("Failed to save [{}] ids: {}", collection, itemIds, e)
                emptyList<Item>()
            }
        }.forEach { item ->
            saveItem(item)
        }
        log.info("Scan NFT's for address [${address.formatted}] complete!")
    }


    private suspend fun processItems(
        collection: String,
        itemIds: List<Long>,
        objectMapper: ObjectMapper,
        address: FlowAddress
    ): List<Item?> {
        return when (collection) {
            "TopShot" -> {
                val contract = contract("0xTOPSHOTTOKEN", "TopShot")
                val items = itemRepository.findAllByIdIn(
                    itemIds.map { ItemId(contract, it) }.toSet()
                ).toIterable().associateBy { it.tokenId }
                itemIds.pmap { tokenId ->
                    val item = items[tokenId]
                    if (item == null) {
                        val momentData = scriptExecutor.executeFile(
                            "/script/get_topshot_moment.cdc",
                            {
                                arg { address(address.bytes) }
                                arg { uint64(tokenId) }
                            }, { json ->
                                dictionaryMap(json) { k, v ->
                                    string(k) to long(v)
                                }
                            })
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
                itemIds.pmap { tokenId ->
                    val item = items[tokenId]
                    if (item == null) {
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

            "MatrixWorldVoucher" -> {
                val contract = contract("0xMATRIXWORLD", "MatrixWorldVoucher")
                val items = itemRepository.findAllByIdIn(
                    itemIds.map { ItemId(contract, it) }.toSet()
                ).toIterable().associateBy { it.tokenId }
                itemIds.pmap { tokenId ->
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
                val contract = contract("0xMATRIXWORLD", "MatrixWorldVoucher")
                val items = itemRepository.findAllByIdIn(
                    itemIds.map { ItemId(contract, it) }.toSet()
                ).toIterable().associateBy { it.tokenId }
                itemIds.pmap { tokenId ->
                    val item = items[tokenId]
                    if (item == null) {
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
                }
            }

            "CNN_NFT" -> {
                val contract = contract("0xCNNNFT", "CNN_NFT")
                val items = itemRepository.findAllByIdIn(
                    itemIds.map { ItemId(contract, it) }.toSet()
                ).toIterable().associateBy { it.tokenId }
                itemIds.pmap { tokenId ->
                    val item = items[tokenId]
                    if (item == null) {
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
}
