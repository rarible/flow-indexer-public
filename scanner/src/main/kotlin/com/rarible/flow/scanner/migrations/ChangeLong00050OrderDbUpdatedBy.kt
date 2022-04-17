package com.rarible.flow.scanner.migrations

import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.repository.OrderFilter
import com.rarible.flow.core.repository.OrderRepository
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Update
import java.time.Instant

@ChangeUnit(
    id = "ChangeLong00050OrderDbUpdatedBy",
    order = "00050",
    author = "flow",
)
class ChangeLong00050OrderDbUpdatedBy(
    private val mongoTemplate: MongoTemplate,
    private val orderRepository: OrderRepository,
) {

    @Execution
    fun changeSet() = runBlocking {
        orderRepository.update(OrderFilter.All, Update().set(Order::dbUpdatedAt.name, Instant.now()))
    }

    @RollbackExecution
    fun rollback() {
    }
}
