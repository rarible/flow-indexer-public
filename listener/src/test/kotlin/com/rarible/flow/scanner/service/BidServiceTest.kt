package com.rarible.flow.scanner.service

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.Balance
import com.rarible.flow.core.domain.OrderStatus
import com.rarible.flow.core.repository.OrderFilter
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.test.Data
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@ExperimentalCoroutinesApi
internal class BidServiceTest : FunSpec({
    val order1 = Data.createOrder()
    val order2 = Data.createOrder()

    val repository = mockk<OrderRepository>("orderRepository") {
        every {
            search(any(), isNull(), any(), OrderFilter.Sort.LATEST_FIRST)
        } returns Flux.fromIterable(
            listOf(order1)
        )

        every {
            search(any(), eq(OrderFilter.Sort.LATEST_FIRST.nextPage(order1)), any(), OrderFilter.Sort.LATEST_FIRST)
        } returns Flux.fromIterable(
            listOf(order2)
        )

        every {
            search(any(), eq(OrderFilter.Sort.LATEST_FIRST.nextPage(order2)), any(), OrderFilter.Sort.LATEST_FIRST)
        } returns Flux.empty()

        every {
            save(any())
        } answers { Mono.just(arg(0)) }

        every {
            defaultSort()
        } returns OrderFilter.Sort.LATEST_FIRST
    }

    test("should deactivate bids by balance") {
        val order = BidService(repository).deactivateBidsByBalance(
            Balance(FlowAddress("0x01"), "A.1234.FlowToken", 11.3.toBigDecimal())
        ).toList().first()

        order.makeStock shouldBe 11.3.toBigDecimal()
        order.status shouldBe OrderStatus.INACTIVE

        coVerify {
            repository.search(any(), null, 1000, OrderFilter.Sort.LATEST_FIRST)
            repository.search(any(), OrderFilter.Sort.LATEST_FIRST.nextPage(order1), 1000, OrderFilter.Sort.LATEST_FIRST)
            repository.save(
                withArg {
                    it.makeStock shouldBe 11.3.toBigDecimal()
                    it.status shouldBe OrderStatus.INACTIVE
                }
            )
        }
    }

    test("should activate bids by balance") {
        val order = BidService(repository).activateBidsByBalance(
            Balance(FlowAddress("0x01"), "A.1234.FlowToken", 11.3.toBigDecimal())
        ).toList().first()

        order.makeStock shouldBe order.make.value
        order.status shouldBe OrderStatus.ACTIVE

        coVerify {
            repository.search(any(), null, 1000, OrderFilter.Sort.LATEST_FIRST)
            repository.search(any(), OrderFilter.Sort.LATEST_FIRST.nextPage(order1), 1000, OrderFilter.Sort.LATEST_FIRST)
            repository.save(
                withArg {
                    it.makeStock shouldBe it.make.value
                    it.status shouldBe OrderStatus.ACTIVE
                }
            )
        }
    }
})
