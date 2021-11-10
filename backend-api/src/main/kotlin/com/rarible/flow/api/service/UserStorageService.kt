package com.rarible.flow.api.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.cadence.JsonCadenceBuilder
import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.rarible.flow.api.metaprovider.RaribleNFT
import com.rarible.flow.core.config.AppProperties
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OwnershipRepository
import kotlinx.coroutines.reactor.awaitSingle
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
    private val ownershipRepository: OwnershipRepository
) {

    private val log: Logger = LoggerFactory.getLogger(UserStorageService::class.java)

    suspend fun scanNFT(address: FlowAddress) {
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

        data.forEach { entry ->
            when (entry.key) {
                "TopShot" -> {
                    entry.value.forEach {
                        val contract = contract("0xTOPSHOTTOKEN", "TopShot")
                        if (notExistsItem(contract, it)) {
                            val res = scriptExecutor.execute(
                                scriptText("/script/get_topshot_moment.cdc"),
                                mutableListOf(builder.address(address.bytes), builder.uint64(it))
                            )
                            val momentData = parser.dictionaryMap(res.jsonCadence) { k, v ->
                                string(k) to long(v)
                            }
                            val item = Item(
                                contract = contract,
                                tokenId = it,
                                creator = contractAddress("0xTOPSHOTTOKEN"),
                                royalties = listOf(
                                    Part(
                                        address = contractAddress("0xTOPSHOTTOKEN"),
                                        fee = 5.0
                                    )
                                ),
                                owner = address,
                                mintedAt = Instant.now(),
                                meta = ObjectMapper().writeValueAsString(momentData),
                                collection = contract,
                                updatedAt = Instant.now()
                            )
                            saveItem(item)
                        } else {
                            val item =
                                itemRepository.findById(ItemId(contract, it)).awaitSingle()
                            saveItem(item.copy(owner = address, updatedAt = Instant.now()))
                        }
                    }
                }
                "MotoGPCard" -> {
                    entry.value.forEach {
                        val contract = contract("0xMOTOGPTOKEN", "MotoGPCard")
                        val item = if (notExistsItem(contract, it)) {
                            Item(
                                contract = contract,
                                tokenId = it,
                                creator = contractAddress("0xMOTOGPTOKEN"),
                                owner = address,
                                royalties = emptyList(),
                                mintedAt = Instant.now(),
                                meta = "{}",
                                collection = contract,
                                updatedAt = Instant.now()
                            )
                        } else {
                            val i = itemRepository.findById(ItemId(contract, it)).awaitSingle()
                            if (i.owner != address) {
                                i.copy(owner = address, updatedAt = Instant.now())
                            } else null
                        }
                        saveItem(item)
                    }
                }
                "Evolution" -> {
                    entry.value.forEach {
                        val contract = contract("0xEVOLUTIONTOKEN", "Evolution")
                        val item = if (notExistsItem(contract, it)) {
                            val res = scriptExecutor.execute(scriptText("/script/get_evolution_nft.cdc"), mutableListOf(builder.address(address.bytes), builder.uint64(it)))
                            val initialMeta = parser.dictionaryMap(res.jsonCadence) { k, v ->
                                string(k) to int(v)
                            }
                            Item(
                                contract = contract,
                                tokenId = it,
                                creator = contractAddress("0xEVOLUTIONTOKEN"),
                                owner = address,
                                royalties = emptyList(),
                                mintedAt = Instant.now(),
                                meta = ObjectMapper().writeValueAsString(initialMeta),
                                collection = contract,
                                updatedAt = Instant.now()
                            )
                        } else {
                            val i = itemRepository.findById(ItemId(contract, it)).awaitSingle()
                            if (i.owner != address) {
                                i.copy(owner = address, updatedAt = Instant.now())
                            } else null
                        }
                        saveItem(item)
                    }
                }
                "RaribleNFT" -> {
                    entry.value.forEach { tokenId ->
                        val contract = contract("0xRARIBLETOKEN", "RaribleNFT")
                        val item = if (notExistsItem(contract, tokenId)) {
                            val res = scriptExecutor.execute(scriptText("/script/get_rarible_nft.cdc"), mutableListOf(
                                builder.address(address.bytes),
                                builder.uint64(tokenId)
                            ))

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
                                meta = ObjectMapper().writeValueAsString(token.metadata),
                                collection = contract,
                                updatedAt = Instant.now()
                            )
                        } else {
                            val i = itemRepository.findById(ItemId(contract, tokenId)).awaitSingle()
                            if (i.owner != address) {
                                i.copy(owner = address, updatedAt = Instant.now())
                            } else null
                        }
                        saveItem(item)
                    }
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
        if (item != null) {
            val a = itemRepository.save(item).awaitSingle()
            protocolEventPublisher.onItemUpdate(a)
            val o = ownershipRepository.save(
                Ownership(
                    contract = item.contract,
                    tokenId = item.tokenId,
                    creator = item.creator,
                    owner = item.owner!!,
                    date = Instant.now()
                )
            ).awaitSingle()
            protocolEventPublisher.onUpdate(o)
        }
    }

    private fun scriptText(resourcePath: String): String {
        val resource = ClassPathResource(resourcePath)
        return resource.inputStream.bufferedReader().use { it.readText() }
    }
}
