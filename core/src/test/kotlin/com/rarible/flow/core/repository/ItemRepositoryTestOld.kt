package com.rarible.flow.core.repository

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.test.IntegrationTest
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit

@IntegrationTest
class ItemRepositoryTestOld {

    @Autowired
    lateinit var itemRepository: ItemRepository

    @BeforeEach
    fun beforeEach() {
        itemRepository.deleteAll().block()
    }

    @Test
    fun `should save and read item`() = runBlocking<Unit> {
        val item = createItem()
        itemRepository.coSave(item)
        val read = itemRepository.coFindById(item.id)
        read shouldNotBe null
        read!!.id shouldBe item.id
    }

    @Test
    fun `should read all items after lastUpdated date`() = runBlocking<Unit> {
        val db = (1L..5L).map { tokenId ->
            val i = itemRepository.coSave(createItem(tokenId = tokenId))
            delay(10)
            i
        }

        val since = db[0].updatedAt.plus(10, ChronoUnit.MILLIS)
        val items = itemRepository.search(
            ItemFilter.All(lastUpdatedFrom = since),
            null,
            null,
            ItemFilter.Sort.LAST_UPDATE
        ).asFlow().toList()
        items shouldHaveSize 4
        items shouldNotContain db[0]
    }

    @Test
    fun `should read all items before lastUpdated date`() = runBlocking<Unit> {
        val db = (1L..5L).map { tokenId ->
            val i = itemRepository.coSave(createItem(tokenId = tokenId))
            delay(10)
            i
        }

        val before = db[2].updatedAt
        val items = itemRepository.search(
            ItemFilter.All(lastUpdatedTo = before),
            null,
            null,
            ItemFilter.Sort.LAST_UPDATE
        ).asFlow().toList()
        items shouldHaveSize 2
        items shouldNotContain listOf(db[0], db[1])
    }

    @Test
    fun `should read all items between dates`() = runBlocking<Unit> {
        val db = (1L..5L).map { tokenId ->
            val i = itemRepository.coSave(createItem(tokenId = tokenId))
            delay(10)
            i
        }

        val before = db[2].updatedAt
        val after = db[0].updatedAt
        val items = itemRepository.search(
            ItemFilter.All(lastUpdatedFrom = after, lastUpdatedTo = before),
            null,
            null,
            ItemFilter.Sort.LAST_UPDATE
        ).asFlow().toList()
        items shouldHaveSize 2
        items shouldNotContain listOf(db[0], db[1])
    }

    @Test
    fun `should save and find by account`() = runBlocking<Unit> {
        val item = createItem()
        itemRepository.coSave(item)
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
        itemRepository.coSave(createItem())
        itemRepository.coSave(createItem(43))
        val read = itemRepository.coFindAll()

        read.count() shouldBe 2
    }

    fun createItem(tokenId: TokenId = 42) = Item(
        "0x01",
        tokenId,
        FlowAddress("0x01"),
        emptyList(),
        FlowAddress("0x02"),
        Instant.now(Clock.systemUTC()),
        collection = "collection",
        updatedAt = Instant.now()
    )
}
