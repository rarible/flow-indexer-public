package com.rarible.flow.api.controller

import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OrderRepositoryR
import com.rarible.flow.randomAddress
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
import java.time.Instant

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
    private lateinit var orderRepositoryR: OrderRepositoryR

    @BeforeEach
    internal fun setUp() {
        itemRepository.deleteAll().block()
        orderRepositoryR.deleteAll().block()
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
            )
        )

        orderRepositoryR.saveAll(
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
            .uri("/v0.1/items/byOwner?owner=${nftOwner.formatted}")
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
            )
        )

        orderRepositoryR.saveAll(
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
            .uri("/v0.1/items/all")
            .exchange()
            .expectStatus().isOk
            .expectBody(FlowNftItemsDto::class.java)
            .returnResult().responseBody!!

        Assertions.assertNotNull(response)
        Assertions.assertTrue(response.items.isNotEmpty())
        Assertions.assertTrue(response.items.size == 2)

    }


}
