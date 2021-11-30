package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.AddressField
import com.nftco.flow.sdk.cadence.NumberField
import com.nftco.flow.sdk.cadence.OptionalField
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainBlock
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.core.domain.*
import com.rarible.flow.events.EventMessage
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Component
import java.time.Instant

@Component
@ConditionalOnExpression("false")
class TopShotSubscriber : BaseItemHistoryFlowLogSubscriber() {

    private val events = "Withdraw,Deposit,MomentMinted,MomentDestroyed".split(",")

    private val royaltyAddress = mapOf(
        FlowChainId.MAINNET to FlowAddress("0xbd69b6abdfcf4539"),
        FlowChainId.TESTNET to FlowAddress("0x01658d9b94068f3c"),
    )

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to flowDescriptor(
                address = "0b2a3299cc857e29",
                contract = "TopShot",
                events = events,
                startFrom = 7641063L,
                dbCollection = collection
            ),
            FlowChainId.TESTNET to flowDescriptor(
                address = "01658d9b94068f3c",
                contract = "TopShot",
                events = events,
                startFrom = 47831085L,
                dbCollection = collection
            ),
            FlowChainId.EMULATOR to flowDescriptor(
                address = "f8d6e0586b0a20c7",
                contract = "TopShot",
                events = events,
                startFrom = 1L,
                dbCollection = collection
            ),
        )

    override suspend fun activity(block: FlowBlockchainBlock, log: FlowBlockchainLog, msg: EventMessage): BaseActivity {

        val contract = msg.eventId.collection()
        val timestamp = Instant.ofEpochMilli(block.timestamp)
        return when (msg.eventId.eventName) {
            "Withdraw" -> {
                val id: NumberField by msg.fields
                val tokenId = id.toLong()!!
                val from: OptionalField by msg.fields
                WithdrawnActivity(
                    contract = contract,
                    tokenId = tokenId,
                    from = if (from.value == null) null else (from.value as AddressField).value,
                    timestamp = timestamp,
                )
            }
            "Deposit" -> {
                val id: NumberField by msg.fields
                val tokenId = id.toLong()!!
                val to: OptionalField by msg.fields
                DepositActivity(
                    contract = contract,
                    tokenId = tokenId,
                    to = if (to.value == null) null else (to.value as AddressField).value,
                    timestamp = timestamp,
                )
            }
            "MomentMinted" -> {
                val momentID: NumberField by msg.fields
                val playID: NumberField by msg.fields
                val setID: NumberField by msg.fields
                val serialNumber: NumberField by msg.fields
                MintActivity(
                    owner = msg.eventId.contractAddress.formatted,
                    contract = contract,
                    tokenId = momentID.toLong()!!,
                    timestamp = timestamp,
                    value = 1L,
                    metadata = mapOf(
                        "playID" to playID.value.toString(),
                        "setID" to setID.value.toString(),
                        "serialNumber" to serialNumber.value.toString()
                    ),
                    royalties = listOf(
                        Part(royaltyAddress[chainId]!!, 0.05)
                    )
                )
            }
            "MomentDestroyed" -> {
                val id: NumberField by msg.fields
                val tokenId = id.toLong()!!
                BurnActivity(
                    contract = contract,
                    tokenId = tokenId,
                    timestamp = timestamp,
                )
            }
            else -> throw IllegalStateException("Unsupported eventId: ${msg.eventId}")
        }
    }
}
