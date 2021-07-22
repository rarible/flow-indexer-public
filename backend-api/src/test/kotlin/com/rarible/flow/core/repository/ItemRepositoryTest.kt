package com.rarible.flow.core.repository

import com.rarible.core.test.ext.MongoTest
import com.rarible.flow.core.config.CoreConfig
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.TokenId
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.onflow.sdk.FlowAddress
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.Instant

@MongoTest
@DataMongoTest(properties = [
    "application.environment = dev",
    "spring.cloud.service-registry.auto-registration.enabled = false",
    "spring.cloud.discovery.enabled = false",
    "spring.cloud.consul.config.enabled = false",
    "logging.logstash.tcp-socket.enabled = false"
])
@ContextConfiguration(classes = [CoreConfig::class])
@ActiveProfiles("test")
internal class ItemRepositoryTest {
    @Autowired
    lateinit var mongo: ReactiveMongoTemplate

    @Autowired
    lateinit var itemRepository: ItemRepository

    @BeforeEach
    fun beforeEach() {
        itemRepository.deleteAll().block()
    }

    @Test
    fun `should save and read item`() = runBlocking<Unit> {
        val item = createItem()
        coSave(itemRepository,item)
        val read = coFindById(itemRepository,item.id)
        read shouldNotBe null
        read!!.id shouldBe item.id
    }

    @Test
    fun `should save and find by account`() = runBlocking<Unit> {
        val item = createItem()
        coSave(itemRepository,item)
        var read = itemRepository.findAllByCreator(FlowAddress("0x01")).asFlow()

        read.count() shouldBe 1
        read.collect {
            it.id shouldBeEqualToComparingFields item.id
        }

        read = itemRepository.findAllByCreator(FlowAddress("0x02")).asFlow()
        read.count() shouldBe 0
    }

    @Test
    fun `should save and find all`() = runBlocking<Unit> {
        coSave(itemRepository,createItem())
        coSave(itemRepository,createItem(43))
        val read = coFindAll(itemRepository)

        read.count() shouldBe 2
    }

    fun createItem(tokenId: TokenId = 42) = Item(
        FlowAddress("0x01"),
        tokenId,
        FlowAddress("0x01"),
        emptyList(),
        FlowAddress("0x02"),
        Instant.now()
    )
}
