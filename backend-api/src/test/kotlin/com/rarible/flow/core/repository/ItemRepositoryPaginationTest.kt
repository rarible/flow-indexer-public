package com.rarible.flow.core.repository

import com.rarible.core.test.ext.MongoTest
import com.rarible.flow.core.config.CoreConfig
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.log.Log
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.onflow.sdk.FlowAddress
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.Instant
import java.time.temporal.ChronoUnit

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
internal class ItemRepositoryPaginationTest(
    @Autowired val itemRepository: ItemRepository
) {

    @BeforeEach
    fun beforeEach() {
        itemRepository.deleteAll().block()
    }

    @Test
    fun `should save and find by account`() = runBlocking<Unit> {
        log.info("Starting...")

        val item1Owner1 = createItem().copy(date = Instant.now().minus(1, ChronoUnit.DAYS))
        itemRepository.coSave(item1Owner1)

        val item2Owner1 = createItem(43)
        itemRepository.coSave(item2Owner1)

        val item1Owner2 = createItem(44).copy(owner = FlowAddress("0x0300"))
        itemRepository.coSave(item1Owner2)
        log.info("Set up items.")

        //owner1 - read the latest
        var read = itemRepository.search(ItemFilter.ByOwner(item1Owner1.owner), null, 1)
        read.count() shouldBe 1
        read.collect {
            it.id shouldBe item2Owner1.id
        }
        log.info("Step 1 done")

        //owner1 - read next and the last
        read = itemRepository.search(ItemFilter.ByOwner(item1Owner1.owner), Continuation(item2Owner1.date, item2Owner1.id), 1)
        read.count() shouldBe 1
        read.collect {
            it.id shouldBe item1Owner1.id
        }
        log.info("Step 2 done")

        //owner1 - try to read more
        read = itemRepository.search(ItemFilter.ByOwner(item1Owner1.owner), Continuation(item1Owner1.date, item1Owner1.id), 1)
        read.count() shouldBe 0
        log.info("Step 3 done")

        //another owner
        read = itemRepository.search(ItemFilter.ByOwner(item1Owner2.owner), null, 100)
        read.count() shouldBe 1
        read.collect {
            it.id shouldBe item1Owner2.id
        }
        log.info("Step 4 done")
    }

    @Test
    fun `should save and find by creator`() = runBlocking<Unit> {
        log.info("Starting...")
        val item1 = createItem().copy(date = Instant.now().minus(1, ChronoUnit.DAYS))
        itemRepository.coSave(item1)

        val item2 = createItem(43)
        itemRepository.coSave(item2)

        val anotherCreator = createItem(44).copy(creator = FlowAddress("0x0300"))
        itemRepository.coSave(anotherCreator)
        log.info("Set up items.")

        //owner1 - read the latest
        var read = itemRepository.search(ItemFilter.ByCreator(item1.creator), null, 1)
        read.count() shouldBe 1
        read.collect {
            it.id shouldBe item2.id
        }

        //creator1 - read next and the last
        read = itemRepository.search(ItemFilter.ByCreator(item1.creator), Continuation(item2.date, item2.id), 1)
        read.count() shouldBe 1
        read.collect {
            it.id shouldBe item1.id
        }

        //creator1 - try to read more
        read = itemRepository.search(ItemFilter.ByCreator(item1.creator), Continuation(item1.date, item1.id), 1)
        read.count() shouldBe 0

        //another creator
        read = itemRepository.search(ItemFilter.ByCreator(anotherCreator.creator), null, 100)
        read.count() shouldBe 1
        read.collect {
            it.id shouldBe anotherCreator.id
        }
    }

    @Test
    fun `should save and find all`() = runBlocking<Unit> {
        val item1 = createItem().copy(date = Instant.now().minus(1, ChronoUnit.DAYS))
        itemRepository.coSave(item1)

        val item2 = createItem(43)
        itemRepository.coSave(item2)

        //read the latest
        var read = itemRepository.search(ItemFilter.All, null, 1)
        read.count() shouldBe 1
        read.collect {
            it.id shouldBe item2.id
        }
        log.info("Step 1 done")

        //read next and the last
        read = itemRepository.search(ItemFilter.All, Continuation(item2.date, item2.id), 1)
        read.count() shouldBe 1
        read.collect {
            it.id shouldBe item1.id
        }
        log.info("Step 2 done")

        //try to read more
        read = itemRepository.search(ItemFilter.All, Continuation(item1.date, item1.id), 1)
        read.count() shouldBe 0
        log.info("Step 3 done")
    }

    fun createItem(tokenId: TokenId = 42) = Item(
        FlowAddress("0x01"),
        tokenId,
        FlowAddress("0x01"),
        emptyList(),
        FlowAddress("0x02"),
        Instant.now()
    )

    companion object {
        val log by Log()
    }
}
