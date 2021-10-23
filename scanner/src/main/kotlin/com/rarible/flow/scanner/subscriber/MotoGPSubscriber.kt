package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.AddressField
import com.nftco.flow.sdk.cadence.NumberField
import com.nftco.flow.sdk.cadence.OptionalField
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainBlock
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.core.domain.*
import com.rarible.flow.events.EventMessage
import org.springframework.stereotype.Component

@Component
class MotoGPSubscriber : BaseItemHistoryFlowLogSubscriber() {

    override val descriptors: Map<FlowChainId, FlowDescriptor>
        get() = mapOf(
            FlowChainId.MAINNET to FlowDescriptor(
                id = "MotoGPCardDescriptor",
                events = setOf(
                    "A.a49cc0ee46c54bfb.MotoGPCard.Mint",
                    "A.a49cc0ee46c54bfb.MotoGPCard.Withdraw",
                    "A.a49cc0ee46c54bfb.MotoGPCard.Deposit",
                    "A.a49cc0ee46c54bfb.MotoGPCard.Burn"
                ),
                collection = collection,
                startFrom = 16246182L
            ),
            FlowChainId.TESTNET to FlowDescriptor(
                id = "MotoGPCardDescriptor",
                events = setOf(
                    "A.01658d9b94068f3c.MotoGPCard.Mint",
                    "A.01658d9b94068f3c.MotoGPCard.Withdraw",
                    "A.01658d9b94068f3c.MotoGPCard.Deposit",
                    "A.01658d9b94068f3c.MotoGPCard.Burn"
                ),
                collection = collection,
                startFrom = 47330085L
            ),
            FlowChainId.EMULATOR to FlowDescriptor(id = "MotoGPCardDescriptor", events = emptySet(), collection = collection, startFrom = 1L)
        )

    override fun activity(block: FlowBlockchainBlock, log: FlowBlockchainLog, msg: EventMessage): FlowActivity {
        val id: NumberField by msg.fields
        val tokenId = id.toLong()!!
        val contract = msg.eventId.collection()
        val timestamp = msg.timestamp
        return when (msg.eventId.eventName) {
            "Mint" -> {
                MintActivity(
                    owner = msg.eventId.contractAddress.formatted,
                    contract = contract,
                    tokenId = tokenId,
                    timestamp = timestamp,
                    value = 1L, royalties = emptyList(), metadata = emptyMap()
                )
            }
            "Withdraw" -> {
                val from: OptionalField by msg.fields
                WithdrawnActivity(
                    contract = contract,
                    tokenId = tokenId,
                    from = if (from.value == null) null else {(from.value as AddressField).value},
                    timestamp = timestamp
                )
            }
            "Deposit" -> {
                val to: OptionalField by msg.fields
                DepositActivity(
                    contract = contract,
                    tokenId = tokenId,
                    to = if (to.value == null) null else {(to.value as AddressField).value},
                    timestamp = timestamp
                )

            }
            "Burn" -> {
                BurnActivity(
                    contract = contract,
                    tokenId = tokenId,
                    timestamp = timestamp
                )
            }
            else -> throw IllegalStateException("Unsupported eventId: ${msg.eventId}" )
        }
    }
}
