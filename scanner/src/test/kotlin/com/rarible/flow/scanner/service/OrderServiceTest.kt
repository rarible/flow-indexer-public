package com.rarible.flow.scanner.service

import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.scanner.Data
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

internal class OrderServiceTest: FunSpec({

    test("should list order") {
        val orderRepository = mockk<OrderRepository>("orderRepository") {
            every {
                findById(any<Long>())
            } returns Mono.empty()

            every {
                save(any())
            } answers { Mono.just(arg(0)) }
        }
        val itemRepository = mockk<ItemRepository>("itemRepository") {
            every {
                findById(any<ItemId>())
            } returns Mono.empty()
        }
        val service = OrderService(orderRepository, itemRepository)
        val activiy = FlowNftOrderActivityList(
            price = BigDecimal("13.37"),
            priceUsd = BigDecimal("26.74"),
            contract = "RaribleOrder",
            tokenId = 13,
            timestamp = Instant.now(),
            hash = 1001.toString(),
            maker = "0x01",
            make = FlowAssetNFT("RaribleNFT", BigDecimal.ONE, 13),
            take = FlowAssetFungible("Flow", BigDecimal("13.37")),
            payments = emptyList()
        )

        service.list(activiy) should { order ->
            order.itemId shouldBe ItemId("RaribleNFT", 13)
        }

        verify {
            orderRepository.findById(1001L)
        }
    }

    test("deactivateOrdersByItem - empty orders") {
        val orderRepository = mockk<OrderRepository>() {
            every {
                findAllByMakeAndMakerAndStatusAndLastUpdatedAtIsBefore(any(), any(), any(), any())
            } returns Flux.empty()
        }
        val itemRepository = mockk<ItemRepository>("itemRepository") {}
        OrderService(orderRepository, itemRepository)
            .deactivateOrdersByItem(Data.createItem(), LocalDateTime.now())
            .count() shouldBe 0

        verify (exactly = 0) {
            orderRepository.save(any())
        }
    }

    test("deactivateOrdersByItem - some orders") {
        val orderRepository = mockk<OrderRepository>() {
            every {
                findAllByMakeAndMakerAndStatusAndLastUpdatedAtIsBefore(
                    any(), any(), eq(OrderStatus.ACTIVE), any()
                )
            } returns Flux.fromIterable(
                listOf(
                    Data.createOrder(),
                )
            )

            every { save(any()) } answers { Mono.just(arg(0)) }
        }
        val itemRepository = mockk<ItemRepository>("itemRepository") {}

        OrderService(orderRepository, itemRepository)
            .deactivateOrdersByItem(Data.createItem(), LocalDateTime.now())
            .count() shouldBe 1

        verify(exactly = 1) {
            orderRepository.save(any())
        }
    }

})