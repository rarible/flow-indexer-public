package com.rarible.flow.api.controller

import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.randomAddress
import com.rarible.flow.randomLong
import com.rarible.protocol.dto.FlowNftItemsDto
import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.onflow.sdk.FlowAddress
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@SpringBootTest(
    properties = [
        "application.environment = dev",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "spring.cloud.consul.config.enabled = false",
        "logging.logstash.tcp-socket.enabled = false",
    ],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient(timeout = "60000")
@ActiveProfiles("test")
class NftOrderItemControllerTest {

    @Autowired
    private lateinit var client: WebTestClient

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @BeforeEach
    internal fun setUp() {
        itemRepository.deleteAll().block()
        orderRepository.deleteAll().block()
    }

    @Test
    fun `should return 1 item by owner`() {
        val nftContract = FlowAddress(randomAddress())
        val nftOwner = FlowAddress(randomAddress())
        val tokenId = 42L
        val nftId = ItemId(nftContract, tokenId)
        val item = Item(
            contract = nftContract,
            tokenId = tokenId,
            creator = nftOwner,
            royalties = listOf(),
            owner = nftOwner,
            date = Instant.now(),
            collection = "collection"
        )

        itemRepository.saveAll(
            listOf(item, item.copy(tokenId = 500L), item.copy(tokenId = 600L))
        ).then().block()

        val order = Order(
            id = ObjectId.get(),
            itemId = nftId,
            maker = nftOwner,
            make = FlowAssetNFT(
                contract = nftContract,
                value = 1.toBigDecimal(),
                tokenId = tokenId
            ),
            amount = 1000.toBigDecimal(),
            sellerFee = 0.toBigDecimal(),
            buyerFee = 0.toBigDecimal(),
            data = OrderData(
                payouts = listOf(), originalFees = listOf()
            ),
            collection = item.collection
        )

        orderRepository.saveAll(
            listOf(
                order,
                order.copy(
                    id = ObjectId.get(),
                    taker = FlowAddress(randomAddress()),
                ),
                order.copy(
                    id = ObjectId.get(),
                    maker = FlowAddress(randomAddress())
                )
            )
        ).collectList().block()

        val response = client.get()
            .uri("/v0.1/order/items/byOwner?owner=${nftOwner.formatted}")
            .exchange()
            .expectStatus().isOk
            .expectBody(FlowNftItemsDto::class.java)
            .returnResult().responseBody!!

        Assertions.assertNotNull(response)
        Assertions.assertTrue(response.items.isNotEmpty())
        Assertions.assertTrue(response.items.size == 1)
    }


    @Test
    fun `should return all items on sale`() {
        val nftContract = FlowAddress(randomAddress())
        val nftOwner = FlowAddress(randomAddress())
        val tokenId = 42L
        val nftId = ItemId(nftContract, tokenId)
        val item = Item(
            contract = nftContract,
            tokenId = tokenId,
            creator = nftOwner,
            royalties = listOf(),
            owner = nftOwner,
            date = Instant.now(),
            collection = "collection"
        )

        itemRepository.saveAll(
            listOf(item, item.copy(tokenId = 500L), item.copy(tokenId = 600L))
        ).then().block()

        val order = Order(
            id = ObjectId.get(),
            itemId = nftId,
            maker = nftOwner,
            make = FlowAssetNFT(
                contract = nftContract,
                value = 1.toBigDecimal(),
                tokenId = tokenId
            ),
            amount = 1000.toBigDecimal(),
            sellerFee = 0.toBigDecimal(),
            buyerFee = 0.toBigDecimal(),
            data = OrderData(
                payouts = listOf(), originalFees = listOf()
            ),
            collection = item.collection
        )

        orderRepository.saveAll(
            listOf(
                order,
                order.copy(
                    id = ObjectId.get(),
                    taker = FlowAddress(randomAddress()),
                ),
                order.copy(
                    id = ObjectId.get(),
                    maker = FlowAddress(randomAddress()),
                    itemId = ItemId(contract = nftContract, tokenId = 500L)
                )
            )
        ).collectList().block()

        val response = client.get()
            .uri("/v0.1/order/items/all")
            .exchange()
            .expectStatus().isOk
            .expectBody(FlowNftItemsDto::class.java)
            .returnResult().responseBody!!

        Assertions.assertNotNull(response)
        Assertions.assertTrue(response.items.isNotEmpty())
        Assertions.assertTrue(response.items.size == 2)

    }


    @Test
    internal fun `should return all items by collection`() {
        val itemId1 = ItemId(contract = FlowAddress(randomAddress()), tokenId = randomLong())
        val itemId2 = ItemId(contract = FlowAddress(randomAddress()), tokenId = randomLong())
        val itemId3 = ItemId(contract = FlowAddress(randomAddress()), tokenId = randomLong())

        val item = Item(
            contract = itemId1.contract,
            tokenId = itemId1.tokenId,
            creator = FlowAddress(randomAddress()),
            royalties = listOf(),
            owner = FlowAddress(randomAddress()),
            date = Instant.now(),
            collection = "CollectionNFT"
        )

        itemRepository.saveAll(
            listOf(
                item,
                item.copy(contract = itemId2.contract, tokenId = itemId2.tokenId),
                item.copy(contract = itemId3.contract, tokenId = itemId3.tokenId),
                item.copy(contract = FlowAddress(randomAddress()), tokenId = randomLong(), collection = "DIFF")
            )
        ).then().block()


        val order = Order(
            id = ObjectId.get(),
            itemId = item.id,
            maker = FlowAddress(randomAddress()),
            make = FlowAssetNFT(
                contract = FlowAddress(randomAddress()),
                BigDecimal.valueOf(1L),
                tokenId = randomLong()
            ),
            buyerFee = BigDecimal.valueOf(1L),
            sellerFee = BigDecimal.valueOf(1L),
            collection = item.collection,
            amount = BigDecimal.valueOf(100L),
            createdAt = LocalDateTime.now(ZoneOffset.UTC),
            data = OrderData(payouts = listOf(), originalFees = listOf())
        )

        orderRepositoryR.saveAll(
            listOf(
                order,
                order.copy(id = ObjectId.get(), itemId = itemId2),
                order.copy(id = ObjectId.get(), itemId = itemId3),
                order.copy(id = ObjectId.get(), itemId = ItemId(contract = FlowAddress(randomAddress()), tokenId = randomLong()),collection = "DIFF"),
            )
        ).then().block()

        client.get().uri("/v0.1/order/items/byCollection?collection={collection}", mapOf("collection" to item.collection))
            .exchange()
            .expectStatus().isOk
            .expectBody<FlowNftItemsDto>()
            .consumeWith {
                Assertions.assertNotNull(it.responseBody)
                val items = it.responseBody!!.items
                Assertions.assertTrue(items.isNotEmpty())
                Assertions.assertTrue(items.size == 3)
            }
    }
}
