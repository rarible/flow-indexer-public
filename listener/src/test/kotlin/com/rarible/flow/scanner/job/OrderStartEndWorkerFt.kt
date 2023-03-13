package com.rarible.flow.scanner.job

import com.ninjasquad.springmockk.MockkBean
import com.rarible.flow.core.converter.OrderToDtoConverter
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.OrderStatus
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.test.Data.createOrder
import com.rarible.flow.scanner.BaseIntegrationTest
import com.rarible.flow.scanner.IntegrationTest
import com.rarible.flow.scanner.config.StartEndWorkerProperties
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant.now

@IntegrationTest
internal class OrderStartEndWorkerFt : BaseIntegrationTest() {

    @Autowired
    lateinit var orderRepository: OrderRepository

    @Autowired
    lateinit var meterRegistry: MeterRegistry

    @Autowired
    lateinit var orderConverter: OrderToDtoConverter

    @MockkBean
    lateinit var protocolEventPublisher: ProtocolEventPublisher

    @BeforeEach
    fun setUp() {
        coEvery { protocolEventPublisher.onOrderUpdate(any(), any(), any()) } returns Unit
    }

    @Test
    fun `should have correct statuses`() = runBlocking<Unit> {
        val job = OrderStartEndWorker(
            orderRepository = orderRepository,
            orderConverter = orderConverter,
            protocolEventPublisher = protocolEventPublisher,
            properties = StartEndWorkerProperties(),
            meterRegistry = meterRegistry
        )

        val expiredOrder = orderRepository.save(
            createOrder().copy(status = OrderStatus.ACTIVE, end = now().minusSeconds(100L).epochSecond)).awaitSingle()
        val notStartedOrder = orderRepository.save(
            createOrder().copy(status = OrderStatus.INACTIVE, start = now().minusSeconds(100L).epochSecond)).awaitSingle()
        val normalOrder = orderRepository.save(createOrder().copy(status = OrderStatus.ACTIVE)).awaitSingle()

        job.update(now())

        checkStatus(expiredOrder, OrderStatus.INACTIVE)
        checkStatus(notStartedOrder, OrderStatus.ACTIVE)
        checkStatus(normalOrder, OrderStatus.ACTIVE)

        coVerify(exactly = 2) { protocolEventPublisher.onOrderUpdate(any(), any(), any()) }
    }

    private suspend fun checkStatus(order: Order, status: OrderStatus) {
        val fetched = orderRepository.findById(order.id).awaitSingle()
        assertThat(fetched.status).isEqualTo(status)
    }

}
