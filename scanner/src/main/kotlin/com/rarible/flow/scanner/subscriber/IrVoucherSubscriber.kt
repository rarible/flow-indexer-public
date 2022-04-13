package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.events.EventId
import org.springframework.stereotype.Component

@Component
class IrVoucherSubscriber : BaseFlowLogEventSubscriber() {
    private val events = mapOf(
        "NFTMinted" to FlowLogType.MINT,
        "NFTBurned" to FlowLogType.BURN,
        "Deposit" to FlowLogType.DEPOSIT,
        "Withdraw" to FlowLogType.WITHDRAW,
    )

    override val descriptors: Map<FlowChainId, FlowDescriptor> = mapOf(
        FlowChainId.MAINNET to flowDescriptor(
            contract = Contracts.IR_VOUCHER,
            chainId = FlowChainId.MAINNET,
            events = events.keys,
            startFrom = 23623167L,
            dbCollection = collection,
        ),
        FlowChainId.TESTNET to flowDescriptor(
            contract = Contracts.IR_VOUCHER,
            chainId = FlowChainId.TESTNET,
            events = events.keys,
            dbCollection = collection,
        ),
        FlowChainId.EMULATOR to flowDescriptor(
            contract = Contracts.IR_VOUCHER,
            chainId = FlowChainId.EMULATOR,
            events = events.keys,
            dbCollection = collection,
        ),
    )

    override suspend fun eventType(log: FlowBlockchainLog) =
        events[EventId.of(log.event.type).eventName]
            ?: throw IllegalStateException("Unsupported event type: ${log.event.type}")
}
