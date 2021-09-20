package com.rarible.flow.scanner

import com.nftco.flow.sdk.FlowEvent
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainBlock
import com.rarible.blockchain.scanner.flow.client.FlowBlockchainLog
import com.rarible.blockchain.scanner.flow.model.FlowDescriptor
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.flow.model.FlowLogRecord
import com.rarible.blockchain.scanner.flow.subscriber.FlowLogEventSubscriber
import com.rarible.blockchain.scanner.framework.model.Log
import com.rarible.flow.scanner.config.ScannerProperties
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ByAccountFlowEventSubscriber(private val scannerProperties: ScannerProperties) : FlowLogEventSubscriber {

    private val descriptor: FlowDescriptor = FlowDescriptor(id = "ByAccountFlowEventSubscriber")

    override fun getDescriptor(): FlowDescriptor = descriptor

    override fun getEventRecords(block: FlowBlockchainBlock, log: FlowBlockchainLog): Flow<FlowLogRecord> {
        if (log.errorMessage.isNullOrEmpty()) {
            val event = log.event!!
            if (isSubscribedAcc(event)) {
                return flowOf(
                    FlowLogRecord(
                        log = FlowLog(
                            transactionHash = log.hash,
                            status = Log.Status.CONFIRMED,
                            txIndex = event.transactionIndex,
                            type = event.type,
                            eventIndex = event.eventIndex,
                            payload = event.payload.stringValue,
                            timestamp = Instant.ofEpochSecond(block.timestamp),
                            blockHeight = block.number,
                        )
                    )
                )
            }
        } else if (scannerProperties.subscribeAllEvents){
            return flowOf(FlowLogRecord(
                log = FlowLog(
                    transactionHash = log.hash,
                    status = Log.Status.CONFIRMED,
                    txIndex = null, eventIndex = null, type = null, payload = null,
                    timestamp = Instant.ofEpochSecond(block.timestamp),
                    blockHeight = block.number,
                    errorMessage = log.errorMessage
                )
            ))
        }
        return emptyFlow()
    }

    private fun isSubscribedAcc(event: FlowEvent): Boolean =
        scannerProperties.subscribeAllEvents ||
        scannerProperties.trackedContracts.any { contract -> event.type.contains(contract, true) }
}
