package com.rarible.flow.core.repository

import com.rarible.core.test.ext.MongoTest
import com.rarible.flow.core.TestBeanConfiguration
import com.rarible.flow.core.config.CoreConfig
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.repository.data.createLegacyOrder
import com.rarible.flow.core.repository.data.createOrder
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.core.query.Update
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
@ContextConfiguration(classes = [CoreConfig::class, TestBeanConfiguration::class])
@ActiveProfiles("test")
internal class OrderRepositoryTest() {

    @Autowired
    lateinit var orderRepository: OrderRepository

    @Autowired
    lateinit var legacyOrderRepository: LegacyOrderRepository

    @BeforeEach
    fun beforeEach() {
        orderRepository.deleteAll().block()
    }

    @Test
    @Deprecated("Delete after the migration")
    fun `order id migration test`() = runBlocking<Unit> {
        val legacy = createLegacyOrder()
        legacyOrderRepository.save(legacy).awaitSingle()

        assertThat(legacyOrderRepository.findById(legacy.id).awaitSingleOrNull()).isNotNull()
        assertThat(orderRepository.findById(legacy.id.toString()).awaitSingleOrNull()).isNull()
        // Ensure conversion works
        assertThat(legacyOrderRepository.findAll().collectList().awaitSingle()[0].id).isEqualTo(legacy.id)
        assertThat(orderRepository.findAll().collectList().awaitSingle()[0].id).isEqualTo(legacy.id.toString())

        assertThat(legacyOrderRepository.findAllByIdIn(listOf(legacy.id)).collectList().awaitSingle()).hasSize(1)
        assertThat(orderRepository.findAllByIdIn(listOf(legacy.id.toString())).collectList().awaitSingle()).hasSize(0)

        legacyOrderRepository.updateIdType()

        // now legacy repo see nothing
        assertThat(legacyOrderRepository.findById(legacy.id).awaitSingleOrNull()).isNull()
        assertThat(orderRepository.findById(legacy.id.toString()).awaitSingleOrNull()).isNotNull()
        // Ensure conversion works to both repos
        assertThat(legacyOrderRepository.findAll().collectList().awaitSingle()[0].id).isEqualTo(legacy.id)
        assertThat(orderRepository.findAll().collectList().awaitSingle()[0].id).isEqualTo(legacy.id.toString())
        // By IDs now works only with strings
        assertThat(legacyOrderRepository.findAllByIdIn(listOf(legacy.id)).collectList().awaitSingle()).hasSize(0)
        assertThat(orderRepository.findAllByIdIn(listOf(legacy.id.toString())).collectList().awaitSingle()).hasSize(1)
    }

    @Test
    fun `should return sell orders by make`() = runBlocking<Unit> {
        val sellOrder = createOrder()
        val bidOrder = createOrder().let {
            it.copy(make = it.take, take = it.make)
        }
        orderRepository.coSaveAll(sellOrder, bidOrder)

        (sellOrder.make as FlowAssetNFT).let { asset ->
            orderRepository
                .findAllByMake(asset.contract, asset.tokenId)
                .collectList()
                .awaitSingle()
                .first().id shouldBe sellOrder.id
        }
    }

    @Test
    fun `should return bid orders by make`() = runBlocking<Unit> {
        val sellOrder = createOrder()
        val bidOrder = createOrder().let {
            it.copy(make = it.take, take = it.make)
        }
        orderRepository.coSaveAll(sellOrder, bidOrder)

        (bidOrder.take as FlowAssetNFT).let { asset ->
            orderRepository
                .findAllByTake(asset.contract, asset.tokenId)
                .collectList()
                .awaitSingle()
                .first().id shouldBe bidOrder.id
        }
    }

    @Test
    fun `should update orders`() = runBlocking<Unit> {
        val sellOrder = createOrder()
        val bidOrder = createOrder().let {
            it.copy(make = it.take, take = it.make)
        }

        orderRepository.coSaveAll(sellOrder, bidOrder)
        orderRepository.update(
            OrderFilter.OnlyBid,
            Update().set(Order::makeStock.name, 0.5.toBigDecimal())
        ) should {
            it.matchedCount shouldBe 1
            it.modifiedCount shouldBe 1
            it.upsertedId shouldBe null
        }

        val updated = orderRepository.search(OrderFilter.OnlyBid, null, null).asFlow().first()
        updated.makeStock shouldBe 0.5.toBigDecimal()

        val skipped = orderRepository.search(OrderFilter.OnlySell, null, null).asFlow().first()
        skipped.makeStock shouldBe BigDecimal.ONE
    }
}
