package com.rarible.flow.api.controller

import com.ninjasquad.springmockk.MockkBean
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.repository.*
import com.rarible.flow.form.MetaForm
import com.rarible.flow.log.Log
import com.rarible.protocol.dto.FlowNftItemDto
import com.rarible.protocol.dto.FlowNftItemsDto
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.slot
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.onflow.sdk.FlowAddress
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import reactor.core.publisher.Mono
import java.net.URI
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
internal class NftApiControllerTest(
    @Autowired val client: WebTestClient
) {
    @MockkBean
    lateinit var itemRepository: ItemRepository

    @MockkBean
    lateinit var itemMetaRepository: ItemMetaRepository

    @Test
    fun `should return all items and stop`() = runBlocking<Unit> {
        val items = listOf(
            createItem(),
            createItem(43).copy(date = Instant.now().minus(1, ChronoUnit.DAYS))
        )
        coEvery {
            itemRepository.search(any(), any(), any())
        } returns items.asFlow()

        val cont = Continuation(Instant.now(), ItemId(FlowAddress("0x01"), 42))
        var response = client
            .get()
            .uri("/v0.1/items/?continuation=$cont")
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowNftItemsDto>()
            .returnResult().responseBody!!
        response.items shouldHaveSize 2

        response = client
            .get()
            .uri("/v0.1/items/?continuation=$cont&size=2")
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowNftItemsDto>()
            .returnResult().responseBody!!
        response.items shouldHaveSize 2

    }

    @Test
    fun `should return item by id`() {
        every {
            itemRepository.findById(any<ItemId>())
        } returns Mono.just(createItem())

        var item = client
            .get()
            .uri("/v0.1/items/{itemId}", mapOf("itemId" to "0x01:42"))
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowNftItemDto>()
            .returnResult().responseBody!!
        item.id shouldBe "0x01:42"
        item.creator shouldBe "0x01"
        item.owner shouldBe "0x02"
    }

    @Test
    fun `should return 404 by id`() {
        every {
            itemRepository.findById(any<ItemId>())
        } returns Mono.empty()

        client
            .get()
            .uri("/v0.1/items/{itemId}", mapOf("itemId" to "0x01:43"))
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `should return items by owner`() {
        val items = listOf(
            createItem(),
            createItem(tokenId = 43).copy(owner = FlowAddress("0x03"))
        )
        val captured = slot<ItemFilter.ByOwner>()
        coEvery {
            itemRepository.search(capture(captured), null, any())
        } coAnswers {
            items.filter { it.owner == captured.captured.owner }.asFlow()
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
        val captured = slot<ItemFilter.ByCreator>()
        coEvery {
            itemRepository.search(capture(captured), null, any())
        } coAnswers {
            items.filter { it.creator == captured.captured.creator }.asFlow()
        }

        var respose = client
            .get()
            .uri("/v0.1/items/byCreator?address={address}", mapOf("address" to "0x01"))
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowNftItemsDto>()
            .returnResult().responseBody!!
        respose.items shouldHaveSize 1
        respose.items[0].owner shouldBe items[0].owner!!.formatted

        respose = client
            .get()
            .uri("/v0.1/items/byCreator?address={address}", mapOf("address" to "0x02"))
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowNftItemsDto>()
            .returnResult().responseBody!!
        respose.items shouldHaveSize 1
        respose.items[0].owner shouldBe items[1].owner!!.formatted

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
    fun `should create meta and return link`() {
        every {
            itemRepository.findById(any<ItemId>())
        } returns Mono.just(createItem())

        val itemCapture = slot<Item>()
        every {
            itemRepository.save(capture(itemCapture))
        } answers {
            Mono.just(itemCapture.captured)
        }

        every {
            itemMetaRepository.findById(any<ItemId>())
        } returns Mono.empty()

        val metaCapture = slot<ItemMeta>()
        every {
            itemMetaRepository.save(capture(metaCapture))
        } answers {
            Mono.just(metaCapture.captured)
        }

        client
            .post()
            .uri("/v0.1/items/meta/0x01:1")
            .bodyValue(
                MetaForm(
                    "title",
                    "description",
                    URI.create("https://keyassets.timeincuk.net/inspirewp/live/wp-content/uploads/sites/34/2021/03/pouring-wine-zachariah-hagy-8_tZ-eu32LA-unsplash-1-920x609.jpg")
                )
            )
            .exchange()
            .expectStatus().isOk
            .expectBody<String>().isEqualTo("/v0.1/items/meta/0x01:1")
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
