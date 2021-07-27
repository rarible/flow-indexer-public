package com.rarible.flow.scanner

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.events.EventMessage
import com.rarible.flow.scanner.config.ScannerProperties
import com.rarible.flow.scanner.model.FlowEvent
import com.rarible.flow.scanner.model.FlowTransaction
import com.rarible.flow.scanner.model.RariEventMessage
import com.rarible.flow.scanner.model.RariEventMessageCaught
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * Catching events described in tracked contracts
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class FlowTxAnalyzer(
    private val flowMapper: ObjectMapper,
    private val scannerProperties: ScannerProperties,
    private val publisher: ApplicationEventPublisher
) {

    /**
     * Analysis of the transaction for events of interest
     */
    fun analyze(tx: FlowTransaction) {
        tx.events.forEachIndexed { index, flowEvent ->
            if (isEventTracked(flowEvent)) {
                val msg = flowMapper.readValue<EventMessage>(flowEvent.data).apply {
                    timestamp = flowEvent.timestamp
                    blockInfo = BlockInfo(
                        transactionId = tx.id,
                        blockHeight = tx.blockHeight,
                        blockId = tx.referenceBlockId
                    )
                }

                publisher.publishEvent(
                    RariEventMessageCaught(
                        RariEventMessage(
                            messageId = "${tx.id}.${index}",
                            event = msg
                        )
                    )
                )
            }
        }
    }

    private fun isEventTracked(event: FlowEvent) =
        scannerProperties.trackedContracts.any { contract -> event.type.contains(contract, true) }
}
