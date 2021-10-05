package com.rarible.flow.listener.service

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.OrderRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDateTime

internal class OrderServiceTest : FunSpec({
    val order = Order(
        1337,
        ItemId("A.12345678.Test", 1),
        FlowAddress("0x1000"),
        null,
        FlowAssetNFT("0x01", 1.toBigDecimal(), 1),
        FlowAssetFungible("FLOW", BigDecimal.TEN),
        1.toBigDecimal(),
        ItemId("0x01", 1).toString(),
        data = OrderData(emptyList(), emptyList()),
        collection = "ABC",
        fill = 13.37.toBigDecimal(),
        lastUpdatedAt = LocalDateTime.now(),
        createdAt = LocalDateTime.now(),
        makeStock = BigInteger.TEN
    )

    val repository = mockk<OrderRepository>("orderRepository") {
        every { findByItemIdAndCancelledAndMaker(any(), any(), any()) } answers { Mono.empty() }
        every { findByItemIdAndCancelledAndMaker(ItemId("A.12345678.Test", 1), any(), FlowAddress("0x1000")) } answers { Mono.just(order) }

        every { save(any()) } answers { Mono.just(arg(0)) }
    }

    val service = OrderService(repository)

    test("should ignore and return null for non-exisisting order") {
        service.cancelOrderByItemIdAndMaker(ItemId("A.12345678.Test_NO", 1), FlowAddress("0x00")) shouldBe null
        verify(exactly = 1) {
            repository.findByItemIdAndCancelledAndMaker(ItemId("A.12345678.Test_NO", 1), false, FlowAddress("0x00"))
        }

        verify(exactly = 0) {
            repository.save(any())
        }
    }

    test("should cancel exisisting order") {
        service.cancelOrderByItemIdAndMaker(ItemId("A.12345678.Test", 1), FlowAddress("0x1000")) should {
            it shouldNotBe null
            it as Order
            it.id shouldBe 1337
            it.cancelled shouldBe true
        }
        verify(exactly = 1) {
            repository.findByItemIdAndCancelledAndMaker(ItemId("A.12345678.Test", 1L), false, FlowAddress("0x1000"))

            repository.save(withArg {
                it.cancelled shouldBe true
            })
        }
    }

})