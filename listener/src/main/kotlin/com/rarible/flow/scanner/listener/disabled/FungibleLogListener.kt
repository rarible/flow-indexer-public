package com.rarible.flow.scanner.listener.disabled

import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.flow.core.converter.OrderToDtoConverter
import com.rarible.flow.core.domain.BalanceHistory
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.BalanceRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.scanner.listener.BalanceFlowLogListener
import com.rarible.flow.scanner.model.Listeners
import com.rarible.flow.scanner.model.SubscriberGroups
import com.rarible.flow.scanner.service.BidService
import com.rarible.protocol.dto.blockchainEventMark
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@ExperimentalCoroutinesApi
class FungibleLogListener(
    private val balanceRepository: BalanceRepository,
    private val bidService: BidService,
    private val protocolEventPublisher: ProtocolEventPublisher,
    private val orderConverter: OrderToDtoConverter,
    environmentInfo: ApplicationEnvironmentInfo
) : BalanceFlowLogListener(
    name = Listeners.FUNGIBLE,
    flowGroupId = SubscriberGroups.BALANCE_HISTORY,
    environmentInfo = environmentInfo
) {

    override suspend fun onLogRecordEvents(events: List<LogRecordEvent>) {
        events
            .map { event -> event.record }
            .filterIsInstance<BalanceHistory>()
            .forEach { history ->
                processBalance(history)
            }
    }

    suspend fun processBalance(history: BalanceHistory) {
        val marks = blockchainEventMark("indexer-in", history.log.timestamp)
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
                protocolEventPublisher.onOrderUpdate(o, orderConverter, marks)
            }
        }
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(FungibleLogListener::class.java)
    }
}
