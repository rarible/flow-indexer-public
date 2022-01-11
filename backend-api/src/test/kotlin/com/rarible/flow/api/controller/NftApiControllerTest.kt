package com.rarible.flow.api.controller

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowException
import com.ninjasquad.springmockk.MockkBean
import com.rarible.flow.api.service.NftItemMetaService
import com.rarible.flow.api.service.NftItemService
import com.rarible.flow.core.converter.ItemToDtoConverter
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.Part
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.repository.ItemMetaRepository
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.protocol.dto.FlowCreatorDto
import com.rarible.protocol.dto.FlowItemMetaDto
import com.rarible.protocol.dto.FlowNftItemDto
import com.rarible.protocol.dto.FlowNftItemRoyaltyDto
import com.rarible.protocol.dto.FlowNftItemsDto
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit

@WebFluxTest(
    controllers = [NftApiController::class],
    properties = [
        "application.environment = dev",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "spring.cloud.consul.config.enabled = false",
        "logging.logstash.tcp-socket.enabled = false"
    ]
)
@AutoConfigureWebTestClient(timeout = "60000")
@ActiveProfiles("test")
internal class NftApiControllerTest {
    @Autowired lateinit var client: WebTestClient

    @MockkBean
    lateinit var nftItemService: NftItemService

    @MockkBean
    lateinit var nftItemMetaService: NftItemMetaService

    @Test
    fun `should return all items and stop`() = runBlocking<Unit> {
        val items = listOf(
            createItem(),
            createItem(tokenId = 43).copy(mintedAt = Instant.now(Clock.systemUTC()).minus(1, ChronoUnit.DAYS))
        )

        coEvery {
            nftItemService.getAllItems(any(), any(), any(), any(), any())
        } returns FlowNftItemsDto(
            total = items.size.toLong(),
            continuation = "",
            items = items.map(ItemToDtoConverter::convert)
        )

        val cont = "${Instant.now().toEpochMilli()}_0x01:42"
        var response = client
            .get()
            .uri("/v0.1/items/all?continuation=$cont")
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowNftItemsDto>()
            .returnResult().responseBody!!
        response.items shouldHaveSize 2

        response = client
            .get()
            .uri("/v0.1/items/all?continuation=$cont&size=2")
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowNftItemsDto>()
            .returnResult().responseBody!!
        response.items shouldHaveSize 2

    }

    @Test
    fun `should return item by id`() = runBlocking<Unit> {
        coEvery {
            nftItemService.getItemById(any())
        } returns ItemToDtoConverter.convert(createItem())

        val item = client
            .get()
            .uri("/v0.1/items/{itemId}", mapOf("itemId" to "A.1234.RaribleNFT:42"))
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowNftItemDto>()
            .returnResult().responseBody!!
        item.id shouldBe "A.1234.RaribleNFT:42"
        item.creators shouldBe listOf(FlowCreatorDto(FlowAddress("0x01").formatted, BigDecimal.ONE))
        item.owner shouldBe FlowAddress("0x02").formatted
    }

    @Test
    fun `should return 404 by id`() {
        coEvery {
            nftItemService.getItemById(any())
        } returns null

        client
            .get()
            .uri("/v0.1/items/{itemId}", mapOf("itemId" to "0x01:43"))
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `should return royalties by item id`() = runBlocking<Unit> {
        coEvery {
            nftItemService.getItemById(any())
        } returns ItemToDtoConverter.convert(createItem())

        client
            .get()
            .uri("/v0.1/items/{itemId}/royalty", mapOf("itemId" to "0x01:42"))
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowNftItemRoyaltyDto>()
    }

    @Test
    fun `should return 404 for royalties by id`() {
        coEvery {
            nftItemService.getItemById(any())
        } returns null

        client
            .get()
            .uri("/v0.1/items/{itemId}/royalty", mapOf("itemId" to "0x01:43"))
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `should return items by owner`() = runBlocking<Unit> {
        val items = listOf(
            createItem(),
            createItem(tokenId = 43).copy(owner = FlowAddress("0x03"))
        )
        coEvery {
            nftItemService.byAccount(any(), any(), any())
        } coAnswers {
            FlowNftItemsDto(
                total = items.filter { it.owner == FlowAddress(arg(0)) }.size.toLong(),
                items = items.filter { it.owner == FlowAddress(arg(0)) }.map(ItemToDtoConverter::convert),
                continuation = ""
            )
        }

        var response = client
            .get()
            .uri("/v0.1/items/byAccount?address={address}", mapOf("address" to "0x02"))
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowNftItemsDto>()
            .returnResult().responseBody!!
        response.items shouldHaveSize 1
        response.items[0].owner shouldBe items[0].owner!!.formatted

        response = client
            .get()
            .uri("/v0.1/items/byAccount?address={address}", mapOf("address" to "0x03"))
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowNftItemsDto>()
            .returnResult().responseBody!!
        response.items shouldHaveSize 1
        response.items[0].owner shouldBe items[1].owner!!.formatted

        response = client
            .get()
            .uri("/v0.1/items/byAccount?address={address}", mapOf("address" to "0x04"))
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowNftItemsDto>()
            .returnResult().responseBody!!
        response.items shouldHaveSize 0

    }

    @Test
    fun `should return items by creator`() {
        val items = listOf(
            createItem(),
            createItem(tokenId = 43).copy(creator = FlowAddress("0x02"))
        )

        coEvery {
           nftItemService.byCreator(any(), any(), any())
        } coAnswers {
            FlowNftItemsDto(
                total = items.filter { it.creator == FlowAddress(arg(0)) }.size.toLong(),
                items = items.filter { it.creator == FlowAddress(arg(0)) }.map(ItemToDtoConverter::convert),
                continuation = ""
            )
        }

        var respose = client
            .get()
            .uri("/v0.1/items/byCreator?address={address}", mapOf("address" to "0x01"))
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowNftItemsDto>()
            .returnResult().responseBody!!
        respose.items shouldHaveSize 1

        respose = client
            .get()
            .uri("/v0.1/items/byCreator?address={address}", mapOf("address" to "0x02"))
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowNftItemsDto>()
            .returnResult().responseBody!!
        respose.items shouldHaveSize 1

        respose = client
            .get()
            .uri("/v0.1/items/byCreator?address={address}", mapOf("address" to "0x04"))
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowNftItemsDto>()
            .returnResult().responseBody!!
        respose.items shouldHaveSize 0

    }

    @Test
    fun `should return items by collection`() {
        val items = listOf(
            createItem(),
            createItem(tokenId = 43).copy(collection = "different collection")
        )

        coEvery {
            nftItemService.byCollection(any(), null, any())
        } coAnswers {
            FlowNftItemsDto(
                total = items.filter { it.collection == arg(0) }.size.toLong(),
                items = items.filter { it.collection == arg(0) }.map(ItemToDtoConverter::convert),
                continuation = ""
            )
        }

        var response = client
            .get()
            .uri("/v0.1/items/byCollection?collection={collection}", mapOf("collection" to "A.1234.RaribleNFT"))
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowNftItemsDto>()
            .returnResult().responseBody!!
        response.items shouldHaveSize 1

        response = client
            .get()
            .uri("/v0.1/items/byCollection?collection={collection}", mapOf("collection" to "different collection"))
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowNftItemsDto>()
            .returnResult().responseBody!!
        response.items shouldHaveSize 1

        response = client
            .get()
            .uri("/v0.1/items/byCollection?collection={collection}", mapOf("collection" to "unsupported collection"))
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowNftItemsDto>()
            .returnResult().responseBody!!
        response.items shouldHaveSize 0


    }

    @Test
    fun `should return all items with filters`() {
        val items = listOf(
            createItem(),
            createItem(tokenId = 43).copy(collection = "different collection")
        )

        coEvery {
            nftItemService.getAllItems(any(), null, any(), any(), any())
        } coAnswers {
            FlowNftItemsDto(
                total = 2L,
                items = items.map(ItemToDtoConverter::convert),
                continuation = ""
            )
        }

        var response = client
            .get()
            .uri("/v0.1/items/all")
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowNftItemsDto>()
            .returnResult().responseBody!!
        response.items shouldHaveSize 2

        response = client
            .get()
            .uri("/v0.1/items/all?showDeleted=true")
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowNftItemsDto>()
            .returnResult().responseBody!!
        response.items shouldHaveSize 2

        response = client
            .get()
            .uri(
                "/v0.1/items/all?lastUpdatedFrom={from}&lastUpdatedTo={to}",
                mapOf("from" to Instant.now().toEpochMilli(), "to" to Instant.now().toEpochMilli()))
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowNftItemsDto>()
            .returnResult().responseBody!!
        response.items shouldHaveSize 2


    }

    @Test
    fun `should return item meta`() {
        val goodItem = ItemId("TEST", 1L)
        coEvery {
            nftItemMetaService.getMetaByItemId(goodItem)
        } returns ItemMeta(goodItem, "good", "good", emptyList(), emptyList())

        client
            .get()
            .uri(
                "/v0.1/items/meta/{itemId}",
                goodItem.toString()
            )
            .exchange().expectStatus().isOk.expectBody<FlowItemMetaDto>()
    }

    @Test
    fun `should return 404 for item meta with script error`() {
        val badItem = ItemId("TEST", 1L)
        coEvery {
            nftItemMetaService.getMetaByItemId(badItem)
        } throws FlowException("Script error")

        client
            .get()
            .uri(
                "/v0.1/items/meta/{itemId}",
                badItem.toString()
            )
            .exchange().expectStatus().isNotFound
    }

    @Test
    fun `should return BAD_REQUEST for item meta with malformed id`() {
        client
            .get()
            .uri(
                "/v0.1/items/meta/{itemId}",
                "malformed"
            )
            .exchange().expectStatus().isBadRequest
    }

    @Test
    fun `should process refresh collection meta`() {
        coEvery {
            nftItemMetaService.getMetaByItemId(any())
        } returns ItemMeta(ItemId("A.1234.RaribleNFT", 1), "good", "good", emptyList(), emptyList())

        coEvery {
            nftItemMetaService.resetMeta(any())
        } returns Unit

        coEvery {
            nftItemService.byCollectionRaw(any(), any(), 1000)
        } returns listOf(
            createItem(),
            createItem(tokenId = 43),
            createItem(tokenId = 1337)
        ).asFlow()

        client
            .put()
            .uri(
                "/v0.1/items/refreshCollectionMeta/{collection}",
                "A.1234.RaribleNFT"
            )
            .exchange().expectStatus().isOk

        coVerifyOrder {
            nftItemService.byCollectionRaw("A.1234.RaribleNFT", null, 1000)
            listOf(42L, 43L, 1337L).forEach { tokenId ->
                nftItemMetaService.resetMeta(ItemId("A.1234.RaribleNFT", tokenId))
                nftItemMetaService.getMetaByItemId(ItemId("A.1234.RaribleNFT", tokenId))
            }
        }
    }

    private fun createItem(collection: String = "A.1234.RaribleNFT", tokenId: TokenId = 42) = Item(
        collection,
        tokenId,
        FlowAddress("0x01"),
        listOf(
            Part(FlowAddress("0x02"), 2.0),
            Part(FlowAddress("0x03"), 10.0),
        ),
        FlowAddress("0x02"),
        Instant.now(Clock.systemUTC()),
        collection = collection,
        updatedAt = Instant.now()
    )
}
