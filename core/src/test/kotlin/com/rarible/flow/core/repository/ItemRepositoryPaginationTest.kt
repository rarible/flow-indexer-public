package com.rarible.flow.core.repository

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.test.IntegrationTest
import com.rarible.flow.core.util.Log
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit

@IntegrationTest
class ItemRepositoryPaginationTest {

    @Autowired
    lateinit var itemRepository: ItemRepository

    @BeforeEach
    fun beforeEach() {
        itemRepository.deleteAll().block()
    }

    @Test
    fun `should save and find by account`() = runBlocking {
        log.info("Starting...")

        val item1Owner1 = createItem().copy(mintedAt = Instant.now(Clock.systemUTC()).minus(1, ChronoUnit.DAYS))
        itemRepository.coSave(item1Owner1)

        val item2Owner1 = createItem(43)
        itemRepository.coSave(item2Owner1)

        val item1Owner2 = createItem(44).copy(owner = FlowAddress("0x0300"))
        itemRepository.coSave(item1Owner2)
        log.info("Set up items.")

        // owner1 - read the latest
        var read = itemRepository.search(
            ItemFilter.ByOwner(item1Owner1.owner!!),
            null,
            1,
            ItemFilter.Sort.LAST_UPDATE
        ).asFlow()
        log.info("Search done")
        read.count() shouldBe 1
        read.collect {
            it.id shouldBe item2Owner1.id
        }
        log.info("Step 1 done")

        // owner1 - read next and the last
        read = itemRepository.search(
            ItemFilter.ByOwner(item1Owner1.owner!!),
            "${item2Owner1.mintedAt.toEpochMilli()}_${item2Owner1.id}",
            1,
            ItemFilter.Sort.LAST_UPDATE
        ).asFlow()
        log.info("Search done")
        read.count() shouldBe 1
        read.collect {
            it.id shouldBe item1Owner1.id
        }
        log.info("Step 2 done")

        // owner1 - try to read more
        read = itemRepository.search(
            ItemFilter.ByOwner(item1Owner1.owner!!),
            "${item1Owner1.mintedAt.toEpochMilli()}_${item1Owner1.id}",
            1,
            ItemFilter.Sort.LAST_UPDATE
        ).asFlow()
        log.info("Search done")
        read.count() shouldBe 0
        log.info("Step 3 done")

        // another owner
        read = itemRepository.search(
            ItemFilter.ByOwner(item1Owner2.owner!!),
            null,
            100,
            ItemFilter.Sort.LAST_UPDATE
        ).asFlow()
        log.info("Search done")
        read.count() shouldBe 1
        read.collect {
            it.id shouldBe item1Owner2.id
        }
        log.info("Step 4 done")
    }

    @Test
    fun `should save and find by creator`() = runBlocking {
        log.info("Starting...")
        val item1 = createItem().copy(mintedAt = Instant.now().minus(1, ChronoUnit.DAYS))
        itemRepository.coSave(item1)

        val item2 = createItem(43)
        itemRepository.coSave(item2)

        val anotherCreator = createItem(44).copy(creator = FlowAddress("0x0300"))
        itemRepository.coSave(anotherCreator)
        log.info("Set up items.")

        // owner1 - read the latest
        var read = itemRepository
            .search(ItemFilter.ByCreator(item1.creator), null, 1, ItemFilter.Sort.LAST_UPDATE)
            .asFlow()
        read.count() shouldBe 1
        read.collect {
            it.id shouldBe item2.id
        }

        // creator1 - read next and the last
        read = itemRepository
            .search(
                ItemFilter.ByCreator(item1.creator), "${item2.mintedAt.toEpochMilli()}_${item2.id}", 1,
                ItemFilter.Sort.LAST_UPDATE
            )
            .asFlow()
        read.count() shouldBe 1
        read.collect {
            it.id shouldBe item1.id
        }

        // creator1 - try to read more
        read = itemRepository
            .search(
                ItemFilter.ByCreator(item1.creator), "${item1.mintedAt.toEpochMilli()}_${item1.id}", 1,
                ItemFilter.Sort.LAST_UPDATE
            )
            .asFlow()
        read.count() shouldBe 0

        // another creator
        read = itemRepository
            .search(ItemFilter.ByCreator(anotherCreator.creator), null, 100, ItemFilter.Sort.LAST_UPDATE)
            .asFlow()
        read.count() shouldBe 1
        read.collect {
            it.id shouldBe anotherCreator.id
        }
    }

    @Test
    fun `should save and find all`() = runBlocking {
        val item1 = createItem().copy(mintedAt = Instant.now(Clock.systemUTC()).minus(1, ChronoUnit.DAYS))
        itemRepository.coSave(item1)

        val item2 = createItem(43)
        itemRepository.coSave(item2)

        val item3 = createItem(44).copy(owner = null)
        itemRepository.coSave(item3)

        // read the latest
        var read = itemRepository
            .search(ItemFilter.All(), null, 1, ItemFilter.Sort.LAST_UPDATE)
            .asFlow()
        read.count() shouldBe 1
        read.collect {
            it.id shouldBe item2.id
        }
        log.info("Step 1 done")

        // read next and the last
        read = itemRepository
            .search(ItemFilter.All(), "${item2.mintedAt.toEpochMilli()}_${item2.id}", 1, ItemFilter.Sort.LAST_UPDATE)
            .asFlow()
        read.count() shouldBe 1
        read.collect {
            it.id shouldBe item1.id
        }
        log.info("Step 2 done")

        // try to read more
        read = itemRepository
            .search(ItemFilter.All(), "${item1.mintedAt.toEpochMilli()}_${item1.id}", 1, ItemFilter.Sort.LAST_UPDATE)
            .asFlow()
        read.count() shouldBe 0
        log.info("Step 3 done")

        read = itemRepository
            .search(ItemFilter.All(), null, null, ItemFilter.Sort.LAST_UPDATE)
            .asFlow()
        read.count() shouldBe 2

        read = itemRepository
            .search(ItemFilter.All(true), null, null, ItemFilter.Sort.LAST_UPDATE)
            .asFlow()
        read.count() shouldBe 3
    }

    private fun createItem(tokenId: TokenId = 42) = Item(
        "0x01",
        tokenId,
        FlowAddress("0x01"),
        emptyList(),
        FlowAddress("0x02"),
        Instant.now(Clock.systemUTC()),
        collection = "collection",
        updatedAt = Instant.now()
    )

    companion object {
        val log by Log()
    }
}
