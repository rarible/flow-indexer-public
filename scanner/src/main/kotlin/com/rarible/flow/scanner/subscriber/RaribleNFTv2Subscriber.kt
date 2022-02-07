package com.rarible.flow.scanner.subscriber

import com.fasterxml.jackson.databind.ObjectMapper
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.*
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.events.EventId
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class RaribleNFTv2Subscriber: BaseFlowLogEventSubscriber() {

    private val events = setOf("Minted", "Withdraw", "Deposit", "Burned")
    private val contractName = "RaribleNFTv2"

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.TESTNET to flowDescriptor(
                address = "ebf4ae01d1284af8",
                events = events,
                contract = contractName,
                dbCollection = collection
            ),
            FlowChainId.EMULATOR to flowDescriptor(
                address = "f8d6e0586b0a20c7",
                events = events,
                contract = contractName,
                dbCollection = collection
            )
        )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType = when(EventId.of(log.event.id).eventName) {
        "Minted" -> FlowLogType.MINT
        "Withdraw" -> FlowLogType.WITHDRAW
        "Deposit" -> FlowLogType.DEPOSIT
        "Burned" -> FlowLogType.BURN
        else -> throw IllegalStateException("Unsupported event type: ${log.event.id}")
    }
}

@JsonCadenceConversion(RaribleNFTv2MetaConverter::class)
data class RaribleNFTv2Meta(
    val name: String,
    val description: String?,
    val cid: String,
    val attributes: Map<String, String>,
    val contentUrls: List<String>,
) {

    fun toMap(): Map<String, String> = mapOf(
        "name" to name,
        "description" to description.orEmpty(),
        "cid" to cid,
        "attributes" to ObjectMapper().writeValueAsString(attributes),
        "contentUrls" to ObjectMapper().writeValueAsString(contentUrls)
    )
}

class RaribleNFTv2MetaConverter : JsonCadenceConverter<RaribleNFTv2Meta> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): RaribleNFTv2Meta =
        unmarshall(value) {
            RaribleNFTv2Meta(
                name = string("name"),
                description = optional("description", JsonCadenceParser::string),
                cid = string("cid"),
                attributes = dictionaryMap("attributes") { key, value -> string(key) to string(value) },
                contentUrls = arrayValues("contentUrls", JsonCadenceParser::unmarshall),
            )
        }

    override fun marshall(value: RaribleNFTv2Meta, namespace: CadenceNamespace): Field<*> =
        marshall {
            struct {
                compositeOfPairs(namespace.withNamespace("RaribleNFTv2.Meta")) {
                    listOf(
                        "name" to string(value.name),
                        "description" to optional(value.description?.let(::string)),
                        "cid" to string(value.cid),
                        "attributes" to dictionaryOfMap {
                            value.attributes.map { (key, value) ->
                                string(key) to string(value)
                            }.toMap()
                        },
                        "contentUrls" to array { value.contentUrls.map(::string) }
                    )
                }
            }
        }
}

@JsonCadenceConversion(RaribleNFTv2RoyaltyConverter::class)
data class RaribleNFTv2Royalty(
    val address: String,
    val fee: BigDecimal,
)

class RaribleNFTv2RoyaltyConverter : JsonCadenceConverter<RaribleNFTv2Royalty> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): RaribleNFTv2Royalty = unmarshall(value) {
        RaribleNFTv2Royalty(address("address"), bigDecimal("fee"))
    }

    override fun marshall(value: RaribleNFTv2Royalty, namespace: CadenceNamespace): Field<*> = marshall {
        struct {
            compositeOfPairs(namespace.withNamespace("RaribleNFTv2.Royalty")) {
                listOf(
                    "address" to address(value.address),
                    "fee" to ufix64(value.fee),
                )
            }
        }
    }
}

