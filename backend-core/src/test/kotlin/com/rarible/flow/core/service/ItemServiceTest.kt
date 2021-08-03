package com.rarible.flow.core.service

import com.rarible.core.test.ext.MongoTest
import com.rarible.flow.core.config.CoreConfig
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.repository.ItemFilter
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.onflow.sdk.FlowAddress
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.Instant

fun createItem(tokenId: TokenId = 42) = Item(
    FlowAddress("0x01"),
    tokenId,
    FlowAddress("0x01"),
    emptyList(),
    FlowAddress("0x02"),
    Instant.now(),
    collection = "collection"
)

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
internal class ItemServiceTest() {
    @Autowired
    lateinit var itemRepository: ItemRepository

    @BeforeEach
    fun beforeEach() {
        itemRepository.deleteAll().block()
    }

    @Test
    fun `should mark as deleted`() = runBlocking {
        val itemService = ItemService(itemRepository)
        var item = createItem()
        itemRepository.coSave(item)

            var items = itemRepository.search(ItemFilter.All, null, null).toList()
            items shouldHaveSize 1

            itemService.markDeleted(item.id)

            item = itemRepository.coFindById(item.id)!!
            item.deleted shouldBe true

            items = itemRepository.search(ItemFilter.All, null, null).toList()
            items shouldHaveSize 0
        }

    @Test
    fun `should mark as unlisted`() = runBlocking {
        val itemService = ItemService(itemRepository)
        var item = createItem().copy(listed = true)
        itemRepository.coSave(item)

            val items = itemRepository.search(ItemFilter.All, null, null).toList()
            items shouldHaveSize 1

            itemService.unlist(item.id)

            item = itemRepository.findById(item.id).awaitFirst()
            item.listed shouldBe false
        }
    }