package com.rarible.flow.scanner

import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.flow.model.FlowLogRecord
import com.rarible.blockchain.scanner.flow.subscriber.FlowLogEventListener
import com.rarible.blockchain.scanner.subscriber.ProcessedBlockEvent
import com.rarible.flow.core.converter.OrderToDtoConverter
import com.rarible.flow.core.domain.BalanceHistory
import com.rarible.flow.core.domain.FlowAssetFungible
import com.rarible.flow.core.domain.OrderStatus
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.*
import com.rarible.flow.log.Log
import com.rarible.flow.scanner.service.BidService
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.reactive.awaitFirstOrDefault
import org.springframework.stereotype.Component

@Component
class FungibleLogListener(
    private val balanceRepository: BalanceRepository,
    private val bidService: BidService,
    private val protocolEventPublisher: ProtocolEventPublisher,
    private val orderConverter: OrderToDtoConverter
) : FlowLogEventListener {

    private val logger by Log()

    override suspend fun onBlockLogsProcessed(blockEvent: ProcessedBlockEvent<FlowLog, FlowLogRecord<*>>) {
        blockEvent
            .records
            .filterIsInstance<BalanceHistory>()
            .forEach { history ->
                processBalance(history)
            }
    }

    private suspend fun processBalance(history: BalanceHistory) {
        val balance = balanceRepository
            .coFindById(history.balanceId)

        if (balance != null && history.date.isAfter(balance.lastUpdatedAt)) {
            logger.info("Updating balance {} with activity {}", balance, history)
            val updatedBalance = balanceRepository.coSave(
                balance.copy(
                    balance = balance.balance + history.change,
                    lastUpdatedAt = history.date
                )
            )

            val updatedBids = if (history.change.signum() == -1) {
                bidService.deactivateBidsByBalance(updatedBalance)
            } else {
                bidService.activateBidsByBalance(updatedBalance)
            }

            updatedBids.collect { o ->
                protocolEventPublisher.onOrderUpdate(o, orderConverter)
            }
        }
    }

    override suspend fun onPendingLogsDropped(logs: List<FlowLogRecord<*>>) {
        /** do nothing */
        logger.warn("onPendingLogsDropped not realized yet!")
    }
}
