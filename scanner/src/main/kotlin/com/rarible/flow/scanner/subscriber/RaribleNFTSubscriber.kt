package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.*
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainBlock
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.core.domain.*
import com.rarible.flow.events.EventMessage
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class RaribleNFTSubscriber: BaseItemHistoryFlowLogSubscriber() {
    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to FlowDescriptor(
                id = "RaribleNFTSubscriber",
                events = setOf(
                    "A.01ab36aaf654a13e.RaribleNFT.Mint",
                    "A.01ab36aaf654a13e.RaribleNFT.Withdraw",
                    "A.01ab36aaf654a13e.RaribleNFT.Deposit",
                    "A.01ab36aaf654a13e.RaribleNFT.Destroy"
                ),
                collection = collection
            ),
            FlowChainId.TESTNET to FlowDescriptor(
                id = "RaribleNFTSubscriber",
                events = setOf(
                    "A.ebf4ae01d1284af8.RaribleNFT.Mint",
                    "A.ebf4ae01d1284af8.RaribleNFT.Withdraw",
                    "A.ebf4ae01d1284af8.RaribleNFT.Deposit",
                    "A.ebf4ae01d1284af8.RaribleNFT.Destroy"
                ),
                collection = collection,
                startFrom = 47831085L
            ),
            FlowChainId.EMULATOR to FlowDescriptor(
                id = "RaribleNFTSubscriber",
                events = setOf(
                    "A.f8d6e0586b0a20c7.RaribleNFT.Mint",
                    "A.f8d6e0586b0a20c7.RaribleNFT.Withdraw",
                    "A.f8d6e0586b0a20c7.RaribleNFT.Deposit",
                    "A.f8d6e0586b0a20c7.RaribleNFT.Destroy"
                ),
                collection = collection,
            ),
        )

    override fun activity(block: FlowBlockchainBlock, log: FlowBlockchainLog, msg: EventMessage): BaseActivity {
        val id: NumberField by msg.fields
        val tokenId = id.toLong()!!
        val contract = msg.eventId.collection()
        val timestamp = Instant.ofEpochMilli(block.timestamp)
        val eventId = "${msg.eventId}"
        return when {
            eventId.endsWith("Mint") -> {
                val mint = Flow.unmarshall(RaribleNftMint::class, log.event.event)
                MintActivity(
                    owner = mint.creator,
                    contract = contract,
                    tokenId = tokenId,
                    timestamp = timestamp,
                    royalties = mint.royalties,
                    metadata = mint.metadata
                )
            }
            eventId.endsWith("Withdraw") -> {
                val from: OptionalField by msg.fields
                WithdrawnActivity(
                    contract = contract,
                    tokenId = tokenId,
                    from = if (from.value == null) null else {(from.value as AddressField).value},
                    timestamp = timestamp
                )
            }
            eventId.endsWith("Deposit") -> {
                val to: OptionalField by msg.fields
                DepositActivity(
                    contract = contract,
                    tokenId = tokenId,
                    to = if (to.value == null) null else {(to.value as AddressField).value},
                    timestamp = timestamp
                )
            }
            eventId.endsWith("Destroy") -> {
                BurnActivity(
                    contract = contract,
                    tokenId = tokenId,
                    timestamp = timestamp
                )
            }
            else -> throw IllegalStateException("Unsupported eventId: $eventId")
        }
    }
}

@JsonCadenceConversion(RaribleNftMintConverter::class)
data class RaribleNftMint(
    val id: Long,
    val creator: String,
    val metadata: Map<String, String>,
    val royalties: List<Part>
)

class RaribleNftMintConverter: JsonCadenceConverter<RaribleNftMint> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): RaribleNftMint = unmarshall(value) {
        RaribleNftMint(
            id = long("id"),
            creator = address("creator"),
            metadata = try {
                dictionaryMap("metadata") { key, value ->
                    string(key) to string(value)
                }
            } catch (_: Exception) {
                mapOf("metaURI" to string("metadata"))
            },
            royalties = arrayValues("royalties") {
                it as StructField
                Part(
                    address = FlowAddress(address(it.value!!.getRequiredField("address"))),
                    fee = double(it.value!!.getRequiredField("fee"))
                )
            }
        )
    }
}
