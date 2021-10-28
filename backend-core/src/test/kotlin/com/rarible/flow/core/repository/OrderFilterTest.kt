package com.rarible.flow.core.repository

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.FlowAsset
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.OrderStatus
import com.rarible.protocol.dto.FlowOrderStatusDto
import io.kotest.core.datatest.forAll
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.springframework.data.mapping.div
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import java.math.BigDecimal

internal class OrderFilterTest : FunSpec({

    test("should make filter - all orders") {
        shouldBeEmpty(OrderFilter.All)
    }

    test("should make filter - by item id") {
        OrderFilter.ByItemId(ItemId("ABC", 1337L)) shouldMakeCriteria (
                Order::itemId isEqualTo ItemId("ABC", 1337L)
                )
    }

    test("should make filter - by currency - null") {
        shouldBeEmpty(OrderFilter.ByCurrency(null))
    }

    test("should make filter - by currency") {
        OrderFilter.ByCurrency(FlowAddress("0x01")) shouldMakeCriteria (
                (Order::take / FlowAsset::contract).isEqualTo("0x0000000000000001")
                )
    }

    test("should make filter - by maker - null") {
        shouldBeEmpty(OrderFilter.ByMaker(null))
    }

    test("should make filter - by maker") {
        OrderFilter.ByMaker(FlowAddress("0x02")) shouldMakeCriteria (
                Order::maker isEqualTo FlowAddress("0x02")
                )
    }

    test("should make filter - by status - null") {
        shouldBeEmpty(OrderFilter.ByStatus(null))
    }

    test("should make filter - by statuses") {
        forAll(
            *OrderStatus.values()
        ) { status ->
            OrderFilter.ByStatus(status) shouldMakeCriteria (
                Order::status inValues listOf(status)
            )
        }
    }

    test("should make filter - by status - multiple") {
        OrderFilter.ByStatus(
            OrderStatus.FILLED,
            OrderStatus.CANCELLED
        ) shouldMakeCriteria (
            Order::status inValues listOf(OrderStatus.FILLED,
                OrderStatus.CANCELLED)
        )
    }

    test("should multiply filters") {
        OrderFilter.ByMaker(FlowAddress("0x01")) *
                OrderFilter.ByCurrency(FlowAddress("0x02")) shouldMakeCriteria Criteria().andOperator(
                    Order::maker isEqualTo FlowAddress("0x01"),
                    Order::take / FlowAsset::contract isEqualTo "0x0000000000000002"
                )
    }


}) {
    companion object {
        fun shouldBeEmpty(filter: OrderFilter) {
            filter.criteria() shouldBe Criteria()
        }

        infix fun OrderFilter.shouldMakeCriteria(criteria: Criteria) {
            this.criteria() shouldBe criteria
        }
    }
}