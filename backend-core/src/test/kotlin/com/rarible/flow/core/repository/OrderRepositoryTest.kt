package com.rarible.flow.core.repository

import com.rarible.core.test.ext.MongoTest
import com.rarible.flow.core.config.CoreConfig
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.OrderData
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.onflow.sdk.FlowAddress
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.math.BigDecimal

@MongoTest
@DataMongoTest(
    properties = [
        "application.environment = dev",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "spring.cloud.consul.config.enabled = false",
        "logging.logstash.tcp-socket.enabled = false"
    ]
)
@ContextConfiguration(classes = [CoreConfig::class])
@ActiveProfiles("test")
internal class OrderRepositoryTest(

) {
    @Autowired
    lateinit var orderRepository: OrderRepository

    @BeforeEach
    fun beforeEach() {
        orderRepository.deleteAll().block()
    }

    @Test
    fun `should return only active orders`() {
        val active = createOrder()
        val cancelled = active.copy(canceled = true, id = 2L)
        val filled = active.copy(fill = 1.toBigDecimal(), id = 3L)
        orderRepository.saveAll(listOf(active, cancelled, filled))

        orderRepository.findActiveById(1L).subscribe {
            it shouldBeEqualToComparingFields active
        }

        orderRepository.findActiveById(2L).subscribe {
            it shouldBe null
        }

        orderRepository.findActiveById(3L).subscribe {
            it shouldBe null
        }
    }


    private fun createOrder() = Order(
        1L,
        ItemId("0x01", 1),
        FlowAddress("0x1000"),
        null,
        FlowAssetNFT("0x01", 1.toBigDecimal(), 1),
        null,
        1.toBigDecimal(),
        ItemId("0x01", 1).toString(),
        buyerFee = BigDecimal.ZERO,
        sellerFee = BigDecimal.ZERO,
        data = OrderData(emptyList(), emptyList()),
        collection = "ABC"
    )
}
