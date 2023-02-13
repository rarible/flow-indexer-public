package com.rarible.flow.scanner

import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.blockchain.scanner.framework.listener.LogRecordEventListener
import com.rarible.flow.core.converter.OrderToDtoConverter
import com.rarible.flow.core.domain.BalanceHistory
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.BalanceRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.scanner.model.SubscriberGroups
import com.rarible.flow.scanner.service.BidService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@ExperimentalCoroutinesApi
@Component
class FungibleLogListener(
    private val balanceRepository: BalanceRepository,
    private val bidService: BidService,
    private val protocolEventPublisher: ProtocolEventPublisher,
    private val orderConverter: OrderToDtoConverter
) : LogRecordEventListener {

    override val groupId: String = SubscriberGroups.BALANCE_HISTORY

    override val id: String = SubscriberGroups.BALANCE_HISTORY

    override suspend fun onLogRecordEvents(events: List<LogRecordEvent>) {
        events
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

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(FungibleLogListener::class.java)
    }
}
