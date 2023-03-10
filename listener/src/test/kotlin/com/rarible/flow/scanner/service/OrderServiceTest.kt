package com.rarible.flow.scanner.service

import com.rarible.flow.core.converter.OrderToDtoConverter
import com.rarible.flow.core.domain.FlowAssetFungible
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.flow.core.domain.FlowNftOrderActivityList
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.OrderStatus
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.test.Data
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.currency.dto.BlockchainDto
import com.rarible.protocol.currency.dto.CurrencyRateDto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

internal class OrderServiceTest: FunSpec({

    val mockRate = 2.3.toBigDecimal()

    val currencyApi = mockk<CurrencyControllerApi>("currencyApi") {
        every {
            getCurrencyRate(eq(BlockchainDto.FLOW), any(), any())
        } returns Mono.just(
            CurrencyRateDto(
                "from", "USD", mockRate, Instant.now()
            )
        )
    }

    val orderConverter = mockk<OrderToDtoConverter>("orderConverter")
    val publisher = mockk<ProtocolEventPublisher>("publisher")
    val itemHistoryRepository = mockk<ItemHistoryRepository>("itemHistoryRepository") {
        every {
            findOrderActivity(any(), any())
        } returns Flux.empty()
    }


    test("should list order") {
        val orderRepository = mockk<OrderRepository>("orderRepository") {
            every {
                findById(any<Long>())
            } returns Mono.empty()

            every {
                save(any())
            } answers { Mono.just(arg(0)) }
        }
        val service = OrderService(orderRepository, itemHistoryRepository, publisher, orderConverter, currencyApi)
        val activity = FlowNftOrderActivityList(
            price = BigDecimal("13.37"),
            priceUsd = BigDecimal("26.74"),
            contract = "RaribleOrder",
            tokenId = 13,
            timestamp = Instant.now(),
            hash = 1001.toString(),
            maker = "0x01",
            make = FlowAssetNFT("RaribleNFT", BigDecimal.ONE, 13),
            take = FlowAssetFungible("Flow", BigDecimal("13.37")),
            estimatedFee = null,
        )

        service.openList(activity, null) should { order ->
            order.itemId shouldBe ItemId("RaribleNFT", 13)
        }

        verify {
            orderRepository.findById(1001L)
        }
    }

    test("deactivateOrdersByItem - empty orders") {
        val orderRepository = mockk<OrderRepository> {
            every {
                findAllByMakeAndMakerAndStatusAndLastUpdatedAtIsBefore(any(), any(), any(), any())
            } returns Flux.empty()
        }
        OrderService(orderRepository, itemHistoryRepository, publisher, orderConverter, currencyApi)
            .deactivateOrdersByItem(Data.createItem(), LocalDateTime.now())
            .count() shouldBe 0

        verify (exactly = 0) {
            orderRepository.save(any())
        }
    }

    test("deactivateOrdersByItem - some orders") {
        val orderRepository = mockk<OrderRepository> {
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

        OrderService(orderRepository, itemHistoryRepository, publisher, orderConverter, currencyApi)
            .deactivateOrdersByItem(Data.createItem(), LocalDateTime.now())
            .count() shouldBe 1

        verify(exactly = 1) {
            orderRepository.save(any())
        }
    }

    test("should update takeUsdPrice") {
        val orderRepository = mockk<OrderRepository> {
            every {
                findAllByStatus(eq(OrderStatus.ACTIVE))
            } returns Flux.fromIterable(
                (1..5).map {
                    Data.createOrder().copy(take = FlowAssetFungible("FlowToken", it.toBigDecimal()))
                }
            )

            every { save(any()) } answers { Mono.just(arg(0)) }
        }

        OrderService(orderRepository, itemHistoryRepository, publisher, orderConverter, currencyApi)
            .updateOrdersPrices()

        verifySequence {
            orderRepository.findAllByStatus(eq(OrderStatus.ACTIVE))
            orderRepository.save( withArg { it.takePriceUsd shouldBe 2.3.toBigDecimal() } )
            orderRepository.save( withArg { it.takePriceUsd shouldBe 4.6.toBigDecimal() } )
            orderRepository.save( withArg { it.takePriceUsd shouldBe 6.9.toBigDecimal() } )
            orderRepository.save( withArg { it.takePriceUsd shouldBe 9.2.toBigDecimal() } )
            orderRepository.save( withArg { it.takePriceUsd shouldBe 11.5.toBigDecimal() } )
        }
    }

})
