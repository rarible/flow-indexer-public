package com.rarible.flow.scanner.migrations

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.FlowActivityType
import com.rarible.flow.core.domain.ItemCollection
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.ItemRepository
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.springframework.data.mapping.div
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where

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

    private val addresses = mapOf(
        FlowChainId.MAINNET to mapOf(
            "topshot" to "",
            "evolution" to "",
            "motogp" to ""
        )
    )

    @Execution
    fun changeSet() {
        runBlocking {
            /*itemRepository.forEach(
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

                itemHistoryRepository.saveAll(itemHistory).then().awaitSingleOrNull()
            }*/

            mongoTemplate.findAll(ItemCollection::class.java).asFlow().collect { collection ->
                val query = Query().addCriteria(
                    where(ItemHistory::activity / BaseActivity::type).isEqualTo(FlowActivityType.MINT)
                        .and("activity.contract").isEqualTo(collection.id)
                )

                val update = Update().apply {
                    val creator = if (collection.name.endsWith("Rarible")) {
                        updateObject["owner"] as String
                    } else {
                        collection.owner.formatted
                    }
                    set("activity.creator", creator)
                }
                mongoTemplate.updateMulti(query, update, ItemHistory::class.java).then().awaitSingleOrNull()
            }
        }
    }

    @RollbackExecution
    fun rollBack() {

    }
}
