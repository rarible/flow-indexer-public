package com.rarible.flow.scanner.subscriber

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainBlock
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.flow.model.FlowLogRecord
import com.rarible.blockchain.scanner.flow.subscriber.FlowLogEventSubscriber
import com.rarible.blockchain.scanner.framework.model.Log
import com.rarible.flow.core.domain.FlowActivity
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.events.EventMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.springframework.beans.factory.annotation.Value
import java.time.Instant

abstract class BaseItemHistoryFlowLogSubscriber : FlowLogEventSubscriber {

    internal val collection = "item_history"

    @Value("\${blockchain.scanner.flow.chainId}")
    lateinit var chainId: FlowChainId

    abstract val descriptors: Map<FlowChainId, FlowDescriptor>

    override fun getEventRecords(block: FlowBlockchainBlock, log: FlowBlockchainLog): Flow<FlowLogRecord<*>> {
        val descriptor = getDescriptor()
        return if (descriptor.events.contains(log.event.id)) {
            val blockTimestamp = Instant.ofEpochMilli(block.timestamp)
            flowOf(
                ItemHistory(
                    log = FlowLog(
                        transactionHash = log.event.transactionId.base16Value,
                        status = Log.Status.CONFIRMED,
                        eventIndex = log.event.eventIndex,
                        eventType = log.event.type,
                        timestamp = blockTimestamp,
                        blockHeight = block.number,
                        blockHash = block.hash
                    ),
                    date = blockTimestamp,
                    activity = activity(
                        block, log,
                        com.nftco.flow.sdk.Flow.unmarshall(EventMessage::class, log.event.event).apply {
                            timestamp = blockTimestamp
                        })
                )
            )
        } else emptyFlow()
    }

    override fun getDescriptor(): FlowDescriptor = descriptors[chainId]!!

    abstract fun activity(block: FlowBlockchainBlock, log: FlowBlockchainLog, msg: EventMessage): FlowActivity
}
