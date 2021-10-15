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
                collection = collection
            ),
            FlowChainId.EMULATOR to FlowDescriptor(id = "MotoGPCardDescriptor", events = emptySet(), collection = collection, startFrom = 1L)
        )

    override fun activity(block: FlowBlockchainBlock, log: FlowBlockchainLog, msg: EventMessage): FlowActivity {
        val idField: Field<*> by msg.fields
        val tokenId = Flow.unmarshall(Long::class, idField)
        val contract = msg.eventId.collection()
        val timestamp = msg.timestamp
        val eventId = "${msg.eventId}"
        return when {
            eventId.endsWith("Mint") -> {
                val mint = Flow.unmarshall(CommonNftMint::class, log.event.event)
                MintActivity(
                    owner = mint.creator,
                    contract = mint.collection,
                    tokenId = tokenId,
                    timestamp = timestamp,
                    royalties = mint.royalties,
                    metadata = mint.metadata
                )
            }
            eventId.endsWith("Withdraw") -> {
                val from: Field<*> by msg.fields
                WithdrawnActivity(
                    contract = contract,
                    tokenId = tokenId,
                    from = Flow.unmarshall(String::class, from),
                    timestamp = timestamp
                )
            }
            eventId.endsWith("Deposit") -> {
                val to: Field<*> by msg.fields
                DepositActivity(
                    contract = contract,
                    tokenId = tokenId,
                    to = Flow.unmarshall(String::class, to),
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

    @JsonCadenceConversion(CommonNftMintConverter::class)
    data class CommonNftMint(
        val id: Long,
        val collection: String,
        val creator: String,
        val metadata: Map<String, String>,
        val royalties: List<Part>
    )

    inner class CommonNftMintConverter: JsonCadenceConverter<CommonNftMint> {
        override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): CommonNftMint = unmarshall(value) {
            CommonNftMint(
                id = long("id"),
                collection = string("collection"),
                creator = address("creator"),
                metadata = dictionaryMap("metadata") { key, value ->
                    string(key) to string(value)
                },
                royalties = arrayValues("royalties") {
                    Part(
                        address = FlowAddress(address("address")),
                        fee = double("fee")
                    )
                }
            )
        }
    }


}
