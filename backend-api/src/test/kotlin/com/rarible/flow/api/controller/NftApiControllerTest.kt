package com.rarible.flow.api.controller

import com.ninjasquad.springmockk.MockkBean
import com.rarible.flow.api.IntegrationTest
import com.rarible.flow.core.domain.Address
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.repository.ItemMetaRepository
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.form.MetaForm
import com.rarible.flow.log.Log
import io.kotest.core.spec.style.FunSpec
import io.mockk.coEvery
import io.mockk.slot
import kotlinx.coroutines.flow.asFlow
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import java.net.URI
import java.time.Instant

//@IntegrationTest
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
        coEvery {
            itemRepository.findAll()
        } returns items.asFlow()

        client
            .get()
            .uri("/v0.1/items/")
            .exchange()
            .expectStatus().isOk
            .expectBodyList<Item>().hasSize(1).contains(items[0])

    }

    @Test
    fun `should return items by owner`() {
        val items = listOf(
            createItem(),
            createItem(tokenId = 43).copy(owner = Address("3"))
        )
        val addressSlot = slot<String>()
        coEvery {
            itemRepository.findAllByAccount(capture(addressSlot))
        } answers {
            items.filter { it.owner.value == addressSlot.captured }.asFlow()
        }

        client
            .get()
            .uri("/v0.1/items/byAccount?address={address}", mapOf("address" to "2"))
            .exchange()
            .expectStatus().isOk
            .expectBodyList<Item>().hasSize(1).contains(items[0])

        client
            .get()
            .uri("/v0.1/items/byAccount?address={address}", mapOf("address" to "3"))
            .exchange()
            .expectStatus().isOk
            .expectBodyList<Item>().hasSize(1).contains(items[1])

        client
            .get()
            .uri("/v0.1/items/byAccount?address={address}", mapOf("address" to "4"))
            .exchange()
            .expectStatus().isOk
            .expectBodyList<Item>().hasSize(0)


    }

    @Test
    fun `should create meta and return link`() {
        coEvery {
            itemMetaRepository.findByItemId(any())
        } returns null

        coEvery {
            itemMetaRepository.save(any())
        } returnsArgument 0

        client
            .post()
            .uri("/v0.1/items/meta/1234")
            .bodyValue(MetaForm("title", "description", URI.create("https://keyassets.timeincuk.net/inspirewp/live/wp-content/uploads/sites/34/2021/03/pouring-wine-zachariah-hagy-8_tZ-eu32LA-unsplash-1-920x609.jpg")))
            .exchange()
            .expectStatus().isOk
            .expectBody<String>().isEqualTo("/v0.1/items/meta/1234")
    }

    fun createItem(tokenId: Int = 42) = Item(
        "1234",
        tokenId,
        Address("1"),
        emptyList(),
        Address("2"),
        Instant.now()
    )

    companion object {
        val log by Log()
    }
}