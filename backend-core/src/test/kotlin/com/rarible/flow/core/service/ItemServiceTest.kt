package com.rarible.flow.core.service

import com.rarible.core.test.ext.MongoTest
import com.rarible.flow.core.config.CoreConfig
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.repository.*
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrDefault
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.onflow.sdk.FlowAddress
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.Clock
import java.time.Instant

fun createItem(tokenId: TokenId = 42) = Item(
    contract = "A.${com.rarible.flow.core.repository.randomAddress()}",
    tokenId,
    FlowAddress("0x01"),
    emptyList(),
    FlowAddress("0x02"),
    Instant.now(Clock.systemUTC()),
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
internal class ItemServiceTest {
    @Autowired
    lateinit var itemRepository: ItemRepository

    @Autowired
    lateinit var ownershipRepository: OwnershipRepository

    @BeforeEach
    fun beforeEach() {
        itemRepository.deleteAll().zipWith(
            ownershipRepository.deleteAll()
        ).subscribe()
    }

    @Test
    fun `should mark as deleted`() = runBlocking {
        val itemService = ItemService(itemRepository, ownershipRepository)
        var item = createItem()
        itemRepository.coSave(item)

        var items = itemRepository.findAll().collectList().awaitFirst()
        items shouldHaveSize 1

        itemService.markDeleted(item.id)

        item = itemRepository.coFindById(item.id)!!
        item.owner shouldBe null

        items = itemRepository.search(ItemFilter.All(), null, null).toList()
        items shouldHaveSize 0
    }

    @Test
    fun `should mark as unlisted`() = runBlocking {
        val itemService = ItemService(itemRepository, ownershipRepository)
        var item = createItem().copy(listed = true)
        itemRepository.coSave(item)

        val items = itemRepository.search(ItemFilter.All(), null, null).toList()
        items shouldHaveSize 1

        itemService.unlist(item.id)

        item = itemRepository.findById(item.id).awaitFirst()
        item.listed shouldBe false
    }

    @Test
    fun `should find alive items`() = runBlocking {
        val itemService = ItemService(itemRepository, ownershipRepository)
        val item1 = createItem().copy(owner = null)
        itemRepository.coSave(item1)

        val item2 = createItem(tokenId = 9000)
        itemRepository.coSave(item2)

        itemService.findAliveById(item1.id) shouldBe null
        itemService.findAliveById(item2.id) shouldNotBe null
    }

    @Test
    fun `should transfer NFT`() = runBlocking {
        val itemService = ItemService(itemRepository, ownershipRepository)
        val before = createItem().copy(listed = true)
        itemRepository.coSave(before)
        val oldOwnership = Ownership(
            contract = before.contract,
            tokenId = before.tokenId,
            owner = before.owner!!,
            date = Instant.now(),
            creators = listOf()
        )
        ownershipRepository.coSave(oldOwnership)

        val newOwner = FlowAddress("0x1111")
        val result = itemService.transferNft(before.id, newOwner)
        result shouldNotBe null

        val item = result!!.first
        item.owner shouldBe newOwner
        item.listed shouldBe false

        val ownerships = ownershipRepository
            .findAllByContractAndTokenIdOrderByDateDescContractDescTokenIdDesc(item.contract, item.tokenId)
            .collectList().awaitFirstOrDefault(emptyList())
        ownerships shouldHaveSize 1
        ownerships[0].owner shouldBe newOwner
        ownerships[0].date shouldBeAfter oldOwnership.date

    }
}
