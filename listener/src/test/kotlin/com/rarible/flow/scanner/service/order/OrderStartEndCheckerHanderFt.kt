package com.rarible.flow.scanner.service.order

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.flow.core.converter.OrderToDtoConverter
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.OrderStatus
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.test.Data.createOrder
import com.rarible.flow.scanner.test.AbstractIntegrationTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant.now

internal class OrderStartEndCheckerHanderFt : AbstractIntegrationTest() {

    @Autowired
    lateinit var orderRepository: OrderRepository

    @Autowired
    lateinit var orderConverter: OrderToDtoConverter

    val protocolEventPublisher: ProtocolEventPublisher = mockk()
    val orderStartedMetric: RegisteredCounter = mockk()
    val orderExpiredMetric: RegisteredCounter = mockk()

    @BeforeEach
    fun setUp() {
        coEvery { protocolEventPublisher.onOrderUpdate(any(), any(), any()) } returns Unit
        coEvery { orderStartedMetric.increment() } returns Unit
        coEvery { orderExpiredMetric.increment() } returns Unit
    }

    @Test
    fun `should have correct statuses`() = runBlocking<Unit> {
        val handler = OrderStartEndCheckerHandler(
            orderRepository = orderRepository,
            orderConverter = orderConverter,
            protocolEventPublisher = protocolEventPublisher,
            orderStartedMetric = orderStartedMetric,
            orderExpiredMetric = orderExpiredMetric,
        )

        val expiredOrder = orderRepository.save(
            createOrder().copy(status = OrderStatus.ACTIVE, end = now().minusSeconds(100L).epochSecond)).awaitSingle()
        val notStartedOrder = orderRepository.save(
            createOrder().copy(status = OrderStatus.INACTIVE, start = now().minusSeconds(100L).epochSecond)).awaitSingle()
        val normalOrder = orderRepository.save(createOrder().copy(status = OrderStatus.ACTIVE)).awaitSingle()

        handler.handle()

        checkStatus(expiredOrder, OrderStatus.CANCELLED)
        checkStatus(notStartedOrder, OrderStatus.ACTIVE)
        checkStatus(normalOrder, OrderStatus.ACTIVE)

        coVerify(exactly = 2) { protocolEventPublisher.onOrderUpdate(any(), any(), any()) }
        coVerify(exactly = 1) { orderStartedMetric.increment() }
        coVerify(exactly = 1) { orderExpiredMetric.increment() }
    }

    private suspend fun checkStatus(order: Order, status: OrderStatus) {
        val fetched = orderRepository.findById(order.id).awaitSingle()
        assertThat(fetched.status).isEqualTo(status)
    }

}
