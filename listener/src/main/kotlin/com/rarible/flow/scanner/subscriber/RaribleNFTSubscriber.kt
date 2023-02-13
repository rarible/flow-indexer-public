package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.*
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.domain.Part
import com.rarible.flow.core.event.EventId
import org.springframework.stereotype.Component

@Component
class RaribleNFTSubscriber: BaseFlowLogEventSubscriber() {

    private val events = setOf("Mint", "Withdraw", "Deposit", "Destroy")
    private val contractName = "RaribleNFT"
    private val name = "rarible_nft"

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to DescriptorFactory.flowNftOrderDescriptor(
                address = "01ab36aaf654a13e",
                contract = contractName,
                events = events,
                startFrom = 19799019L,
                dbCollection = collection,
                name = name,
            ),
            FlowChainId.TESTNET to DescriptorFactory.flowNftOrderDescriptor(
                address = "ebf4ae01d1284af8",
                events = events,
                contract = contractName,
                dbCollection = collection,
                name = name,
            ),
            FlowChainId.EMULATOR to DescriptorFactory.flowNftOrderDescriptor(
                address = "f8d6e0586b0a20c7",
                events = events,
                contract = contractName,
                dbCollection = collection,
                name = name,
            ),
        )

    override suspend fun eventType(log: FlowBlockchainLog): FlowLogType = when(EventId.of(log.event.id).eventName) {
        "Mint" -> FlowLogType.MINT
        "Withdraw" -> FlowLogType.WITHDRAW
        "Deposit" -> FlowLogType.DEPOSIT
        "Destroy" -> FlowLogType.BURN
        else -> throw IllegalStateException("Unsupported event type: ${log.event.id}")
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
