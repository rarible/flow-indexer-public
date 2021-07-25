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
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.slot
import kotlinx.coroutines.flow.asFlow
import org.junit.jupiter.api.Test
import org.onflow.sdk.FlowAddress
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI
import java.time.Instant

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
    fun `should return all items`() {
        val items = listOf(createItem())
        every {
            itemRepository.findAll()
        } returns Flux.fromIterable(items)

        var item = client
            .get()
            .uri("/v0.1/items/")
            .exchange()
            .expectStatus().isOk
            .expectBodyList<FlowNftItemDto>().hasSize(1)
            .returnResult().responseBody!![0]

        item.owner shouldBe items[0].owner.formatted

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
        val addressSlot = slot<FlowAddress>()
        every {
            itemRepository.findAllByOwner(capture(addressSlot))
        } answers {
            Flux.fromIterable(items.filter { it.owner == addressSlot.captured } )
        }

        var item = client
            .get()
            .uri("/v0.1/items/byAccount?address={address}", mapOf("address" to "0x02"))
            .exchange()
            .expectStatus().isOk
            .expectBodyList<FlowNftItemDto>().hasSize(1)
            .returnResult().responseBody!![0]
        item.owner shouldBe items[0].owner.formatted

        item = client
            .get()
            .uri("/v0.1/items/byAccount?address={address}", mapOf("address" to "0x03"))
            .exchange()
            .expectStatus().isOk
            .expectBodyList<FlowNftItemDto>()
            .hasSize(1)
            .returnResult().responseBody!![0]
        item.owner shouldBe items[1].owner.formatted

        client
            .get()
            .uri("/v0.1/items/byAccount?address={address}", mapOf("address" to "0x04"))
            .exchange()
            .expectStatus().isOk
            .expectBodyList<FlowNftItemDto>().hasSize(0)
    }

    @Test
    fun `should return items by creator`() {
        val items = listOf(
            createItem(),
            createItem(tokenId = 43).copy(creator = FlowAddress("0x02"))
        )
        val addressSlot = slot<FlowAddress>()
        every {
            itemRepository.findAllByCreator(capture(addressSlot))
        } answers {
            Flux.fromIterable(items.filter { it.creator == addressSlot.captured })
        }

        var item = client
            .get()
            .uri("/v0.1/items/byCreator?address={address}", mapOf("address" to "0x01"))
            .exchange()
            .expectStatus().isOk
            .expectBodyList<FlowNftItemDto>().hasSize(1)
            .returnResult().responseBody!![0]
        item.owner shouldBe items[0].owner.formatted

        client
            .get()
            .uri("/v0.1/items/byCreator?address={address}", mapOf("address" to "0x02"))
            .exchange()
            .expectStatus().isOk
            .expectBodyList<FlowNftItemDto>().hasSize(1)
            .returnResult().responseBody!![0]
        item.owner shouldBe items[1].owner.formatted

        client
            .get()
            .uri("/v0.1/items/byCreator?address={address}", mapOf("address" to "0x04"))
            .exchange()
            .expectStatus().isOk
            .expectBodyList<FlowNftItemDto>().hasSize(0)

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
            .bodyValue(MetaForm("title", "description", URI.create("https://keyassets.timeincuk.net/inspirewp/live/wp-content/uploads/sites/34/2021/03/pouring-wine-zachariah-hagy-8_tZ-eu32LA-unsplash-1-920x609.jpg")))
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
