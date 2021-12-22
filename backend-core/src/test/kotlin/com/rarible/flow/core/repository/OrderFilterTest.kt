package com.rarible.flow.core.repository

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.FlowAsset
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.OrderStatus
import io.kotest.core.datatest.forAll
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.Headers2
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.data.table
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.springframework.data.domain.Sort
import org.springframework.data.mapping.div
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lte
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.reflect.KProperty

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
        OrderFilter.ByCurrency("A.1234.FUSD") shouldMakeCriteria (
                (Order::take / FlowAsset::contract).isEqualTo("A.1234.FUSD")
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
                Order::status inValues listOf(
                    OrderStatus.FILLED,
                    OrderStatus.CANCELLED
                )
                )
    }

    test("should multiply filters") {
        OrderFilter.ByMaker(FlowAddress("0x01")) *
                OrderFilter.ByCurrency("A.1234.Flow") shouldMakeCriteria Criteria().andOperator(
            Order::maker isEqualTo FlowAddress("0x01"),
            Order::take / FlowAsset::contract isEqualTo "A.1234.Flow"
        )
    }

    test("order filter - sort ") {
        table(
            Headers2("OrderFilter.Sort", "Spring sort"),
            row(
                OrderFilter.Sort.LATEST_FIRST, Sort.by(
                    Sort.Order.desc(Order::createdAt.name),
                    Sort.Order.desc(Order::id.name)
                )
            ),

            row(
                OrderFilter.Sort.EARLIEST_FIRST, Sort.by(
                    Sort.Order.asc(Order::createdAt.name),
                    Sort.Order.asc(Order::id.name)
                )
            )
        ).forAll { sort, springSort ->
            sort.springSort() shouldBe springSort

            val dateTime = LocalDateTime.parse("2021-11-09T10:00:00")
            val entities = flowOf<Order>(
                mockk(), mockk() {
                    every { createdAt } returns dateTime
                    every { id } returns 1000
                }
            )
            sort.nextPage(entities, 3) shouldBe null
            sort.nextPage(entities, 2) shouldBe "${dateTime.toInstant(ZoneOffset.UTC).toEpochMilli()}_1000"
            sort.nextPageSafe(null) shouldBe null
        }

    }

    test("should make filter - by make value") {
        OrderFilter.ByMakeValue(KProperty<BigDecimal>::lte, BigDecimal.TEN).shouldMakeCriteria(
            Order::make / FlowAsset::value lte BigDecimal.TEN
        )

        OrderFilter.ByMakeValue(KProperty<BigDecimal>::gt, BigDecimal.TEN).shouldMakeCriteria(
            Order::make / FlowAsset::value gt BigDecimal.TEN
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