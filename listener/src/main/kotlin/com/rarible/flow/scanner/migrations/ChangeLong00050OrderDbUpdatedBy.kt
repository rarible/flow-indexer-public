package com.rarible.flow.scanner.migrations

import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.repository.OrderFilter
import com.rarible.flow.core.repository.OrderRepository
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.query.Update
import java.time.Instant

//@ChangeUnit(
//    id = "ChangeLong00050OrderDbUpdatedBy",
//    order = "00050",
//    author = "flow",
//)
//class ChangeLong00050OrderDbUpdatedBy(
//    private val orderRepository: OrderRepository,
//) {
//
//    @Execution
//    fun changeSet() = runBlocking<Unit> {
//        orderRepository.update(OrderFilter.All, Update().set(Order::dbUpdatedAt.name, Instant.now()))
//    }
//
//    @RollbackExecution
//    fun rollback() {
//    }
//}
