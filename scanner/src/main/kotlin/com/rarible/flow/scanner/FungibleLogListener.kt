package com.rarible.flow.scanner

import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.flow.model.FlowLogRecord
import com.rarible.blockchain.scanner.flow.subscriber.FlowLogEventListener
import com.rarible.blockchain.scanner.subscriber.ProcessedBlockEvent
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.BalanceRepository
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OrderFilter
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.log.Log
import com.rarible.flow.scanner.model.IndexerEvent
import com.rarible.flow.scanner.service.IndexerEventService
import kotlinx.coroutines.reactive.awaitFirstOrDefault
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Component

@Component
class FungibleLogListener(
    private val balanceRepository: BalanceRepository,
    private val orderRepository: OrderRepository
) : FlowLogEventListener {

    private val logger by Log()

    override suspend fun onBlockLogsProcessed(blockEvent: ProcessedBlockEvent<FlowLog, FlowLogRecord<*>>) {
        blockEvent.records.filterIsInstance<BalanceHistory>()
            .forEach { history ->
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

                    if(history.change.signum() == -1) {
                        orderRepository.search(
                            OrderFilter.OnlyBid * OrderFilter.ByMaker(updatedBalance.account), null, 1000
                        ).collectList().awaitFirstOrDefault(emptyList()).forEach { bid ->
                            logger.info("Found bid [{}] for possible deactivation", bid.id)
                            val make = bid.make as FlowAssetFungible
                            if(make.contract == updatedBalance.token && updatedBalance.balance.compareTo(make.value) == 1) {
                                logger.info("Deactivating bid [{}]...", bid.id)
                                orderRepository.coSave(
                                    bid.copy(status = OrderStatus.INACTIVE)
                                )
                            }
                        }
                    }
                }
            }
    }

    override suspend fun onPendingLogsDropped(logs: List<FlowLogRecord<*>>) {
        /** do nothing */
        logger.warn("onPendingLogsDropped not realized yet!")
    }
}
