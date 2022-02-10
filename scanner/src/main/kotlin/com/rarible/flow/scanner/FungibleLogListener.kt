package com.rarible.flow.scanner

import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.flow.model.FlowLogRecord
import com.rarible.blockchain.scanner.flow.subscriber.FlowLogEventListener
import com.rarible.blockchain.scanner.subscriber.ProcessedBlockEvent
import com.rarible.flow.core.converter.OrderToDtoConverter
import com.rarible.flow.core.domain.BalanceHistory
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.BalanceRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.log.Log
import com.rarible.flow.scanner.service.BidService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component

@ExperimentalCoroutinesApi
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

    suspend fun processBalance(history: BalanceHistory) {
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

            val (action, updatedBids) = if (history.change.signum() == -1) {
                "Deactivated" to bidService.deactivateBidsByBalance(updatedBalance).toList()
            } else {
                "Activated" to bidService.activateBidsByBalance(updatedBalance).toList()
            }

            logger.info(
                "{} bids [{}] by balance {} of address {}",
                action,
                updatedBids.map { it.id },
                updatedBalance.balance,
                updatedBalance.account.formatted
            )
            updatedBids.forEach { o ->
                protocolEventPublisher.onOrderUpdate(o, orderConverter)
            }
        }
    }

    override suspend fun onPendingLogsDropped(logs: List<FlowLogRecord<*>>) {
        /** do nothing */
        logger.warn("onPendingLogsDropped not realized yet!")
    }
}
