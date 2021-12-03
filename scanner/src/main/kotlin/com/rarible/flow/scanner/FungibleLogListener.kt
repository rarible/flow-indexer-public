package com.rarible.flow.scanner

import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.flow.model.FlowLogRecord
import com.rarible.blockchain.scanner.flow.subscriber.FlowLogEventListener
import com.rarible.blockchain.scanner.subscriber.ProcessedBlockEvent
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.BalanceRepository
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.log.Log
import com.rarible.flow.scanner.model.IndexerEvent
import com.rarible.flow.scanner.service.IndexerEventService
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Component

@Component
class FungibleLogListener(
    private val balanceRepository: BalanceRepository
) : FlowLogEventListener {

    private val log by Log()

    override suspend fun onBlockLogsProcessed(blockEvent: ProcessedBlockEvent<FlowLog, FlowLogRecord<*>>) {
        blockEvent.records.filterIsInstance<BalanceHistory>()
            .forEach { history ->
                val balance = balanceRepository
                    .coFindById(history.balanceId)

                if (balance != null && history.date.isAfter(balance.lastUpdatedAt)) {
                    balanceRepository.coSave(
                        balance.copy(
                            balance = balance.balance + history.change,
                            lastUpdatedAt = history.date
                        )
                    )
                }
            }
    }

    override suspend fun onPendingLogsDropped(logs: List<FlowLogRecord<*>>) {
        /** do nothing */
        log.warn("onPendingLogsDropped not realized yet!")
    }
}
