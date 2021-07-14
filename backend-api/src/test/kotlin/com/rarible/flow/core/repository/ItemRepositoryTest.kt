package com.rarible.flow.core.repository

import com.rarible.core.test.ext.MongoTest
import com.rarible.flow.core.config.CoreConfig
import com.rarible.flow.core.domain.Address
import com.rarible.flow.core.domain.Item
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
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

    @Test
    fun `should save and read item`() = runBlocking<Unit> {
        val item = createItem()
        itemRepository.save(item)
        val read = itemRepository.findById(item.id)
        read shouldNotBe null
        read!! shouldBeEqualToComparingFields item
    }

    @Test
    fun `should save and find by account`() = runBlocking<Unit> {
        val item = createItem()
        itemRepository.save(item)
        val read = itemRepository.findAllByAccount("2")

        read.count() shouldBe 1
        read.collect {
            it shouldBeEqualToComparingFields item
        }
    }

    fun createItem(tokenId: Int = 42) = Item(
        "1234",
        tokenId.toLong(),
        Address("1"),
        emptyList(),
        Address("2"),
        Instant.now()
    )
}
