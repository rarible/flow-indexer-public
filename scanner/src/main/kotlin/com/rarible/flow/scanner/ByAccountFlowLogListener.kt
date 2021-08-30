package com.rarible.flow.scanner

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.flow.model.FlowLogRecord
import com.rarible.blockchain.scanner.flow.subscriber.FlowLogEventListener
import com.rarible.blockchain.scanner.subscriber.ProcessedBlockEvent
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.events.EventMessage
import com.rarible.flow.scanner.config.ScannerProperties
import com.rarible.flow.scanner.model.RariEventMessage
import com.rarible.flow.scanner.model.RariEventMessageCaught
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset

@Component
class ByAccountFlowLogListener(
    private val scannerProperties: ScannerProperties,
    private val flowMapper: ObjectMapper,
    private val publisher: ApplicationEventPublisher
    ): FlowLogEventListener {

    private val log: Logger = LoggerFactory.getLogger(ByAccountFlowLogListener::class.java)

    override suspend fun onBlockLogsProcessed(blockEvent: ProcessedBlockEvent<FlowLog, FlowLogRecord>) {
        blockEvent.records.filter { it.log.errorMessage.isNullOrEmpty() }.forEach { flowLogRecord ->
            if (isEventTracked(flowLogRecord.log.type!!)) {
                val flowLog = flowLogRecord.log
                val msg = flowMapper.readValue<EventMessage>(flowLog.payload!!).apply {
                    timestamp = LocalDateTime.ofInstant(flowLog.timestamp, ZoneOffset.UTC)
                    blockInfo = BlockInfo(
                        transactionId = flowLog.transactionHash,
                        blockHeight = flowLog.blockHeight,
                        blockId = blockEvent.event.block.hash
                    )
                }
                publisher.publishEvent(RariEventMessageCaught(message = RariEventMessage(messageId = flowLogRecord.id, event = msg)))
            }
        }
    }

    override suspend fun onPendingLogsDropped(logs: List<FlowLogRecord>) {
        /** do nothing */
    }

    private fun isEventTracked(eventType: String) =
        scannerProperties.trackedContracts.any { contract -> eventType.contains(contract, true) }
}
