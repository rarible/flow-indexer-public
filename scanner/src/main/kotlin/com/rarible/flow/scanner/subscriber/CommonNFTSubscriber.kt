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

@Component
class CommonNFTSubscriber: BaseItemHistoryFlowLogSubscriber() {
    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to FlowDescriptor(
                id = "CommonNFTSubscriber",
                events = setOf(
                    "A.665b9acf64dfdfdb.CommonNFT.Mint",
                    "A.665b9acf64dfdfdb.CommonNFT.Withdraw",
                    "A.665b9acf64dfdfdb.CommonNFT.Deposit",
                    "A.665b9acf64dfdfdb.CommonNFT.Destroy"
                ),
                collection = collection
            ),
            FlowChainId.TESTNET to FlowDescriptor(
                id = "CommonNFTSubscriber",
                events = setOf(
                    "A.01658d9b94068f3c.CommonNFT.Mint",
                    "A.01658d9b94068f3c.CommonNFT.Withdraw",
                    "A.01658d9b94068f3c.CommonNFT.Deposit",
                    "A.01658d9b94068f3c.CommonNFT.Destroy"
                ),
                collection = collection,
                startFrom = 47330085L
            ),
            FlowChainId.EMULATOR to FlowDescriptor(id = "CommonNFTSubscriber", events = emptySet(), collection = collection, startFrom = 1L)
        )

    override fun activity(block: FlowBlockchainBlock, log: FlowBlockchainLog, msg: EventMessage): FlowActivity {
        val id: NumberField by msg.fields
        val tokenId = id.toLong()!!
        val contract = msg.eventId.collection()
        val timestamp = msg.timestamp
        val eventId = "${msg.eventId}"
        return when {
            eventId.endsWith("Mint") -> {
                val mint = Flow.unmarshall(CommonNftMint::class, log.event.event)
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

@JsonCadenceConversion(CommonNftMintConverter::class)
data class CommonNftMint(
    val id: Long,
    val creator: String,
    val metadata: Map<String, String>,
    val royalties: List<Part>
)

class CommonNftMintConverter: JsonCadenceConverter<CommonNftMint> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): CommonNftMint = unmarshall(value) {
        CommonNftMint(
            id = long("id"),
            creator = address("creator"),
            metadata = try {
                dictionaryMap("metadata") { key, value ->
                    string(key) to string(value)
                }
            } catch (_: Exception) {
                mapOf("metaUrl" to string("metadata"))
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
