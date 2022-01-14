package com.rarible.flow.scanner.migrations

import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.FlowActivityType
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.MintActivity
import com.rarible.flow.core.repository.ItemFilter
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coSaveAll
import com.rarible.flow.core.repository.filters.ScrollingSort
import com.rarible.flow.core.repository.forEach
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import kotlinx.coroutines.reactive.awaitFirstOrDefault
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.runBlocking
import org.springframework.data.mapping.div
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import reactor.core.publisher.Flux

@ChangeUnit(
    id = "ChangeLog00012ActivityDataMigration",
    order = "00012",
    author = "flow"
)
class ChangeLog00012ActivityDataMigration(
    private val itemRepository: ItemRepository,
    private val mongoTemplate: ReactiveMongoTemplate,
    private val itemHistoryRepository: ItemHistoryRepository
) {

    @Execution
    fun changeSet() {
        runBlocking {
            itemRepository.forEach(
                ItemFilter.All(true),
                null,
                ScrollingSort.MAX_LIMIT,
                ItemFilter.Sort.LAST_UPDATE
            ) { item ->
                val itemHistory = mongoTemplate.find<ItemHistory>(
                    Query(
                        Criteria().andOperator(
                            ItemHistory::activity / BaseActivity::type isEqualTo FlowActivityType.MINT,
                            Criteria("${ItemHistory::activity.name}.${MintActivity::contract.name}").isEqualTo(item.contract),
                            Criteria("${ItemHistory::activity.name}.${MintActivity::tokenId.name}").isEqualTo(item.tokenId),
                            Criteria("${ItemHistory::activity.name}.${MintActivity::creator.name}").exists(false)
                        )
                    )
                ).map { itemHistory ->
                    itemHistory.copy(
                        activity = (itemHistory.activity as MintActivity).copy(
                            creator = item.creator.formatted
                        )
                    )
                }

                itemHistoryRepository.saveAll(itemHistory).awaitLast()
            }
        }
    }

    @RollbackExecution
    fun rollBack() {

    }
}
