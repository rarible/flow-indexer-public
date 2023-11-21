package com.rarible.flow.api.controller

import com.nftco.flow.sdk.FlowAddress
import com.ninjasquad.springmockk.MockkBean
import com.rarible.flow.api.TestPropertiesConfiguration
import com.rarible.flow.api.http.shouldGetBadRequest
import com.rarible.flow.api.http.shouldGetPaginatedResult
import com.rarible.flow.api.service.OrderService
import com.rarible.flow.core.domain.FlowAssetFungible
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.OrderData
import com.rarible.flow.core.domain.OrderType
import com.rarible.flow.core.domain.Payout
import com.rarible.flow.core.repository.OrderFilter
import com.rarible.flow.randomFlowAddress
import com.rarible.flow.randomLong
import com.rarible.protocol.dto.FlowOrderDto
import com.rarible.protocol.dto.FlowOrderIdsDto
import com.rarible.protocol.dto.FlowOrdersPaginationDto
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

@WebFluxTest(
    controllers = [OrderController::class],
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
@Import(TestPropertiesConfiguration::class)
class OrderControllerTest {

    @MockkBean
    private lateinit var orderService: OrderService

    @Autowired
    lateinit var client: WebTestClient

    @Test
    fun `should respond 404`() {
        coEvery {
            orderService.orderById(any())
        } returns null

        client.get()
            .uri("/v0.1/orders/1")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `should return order by id`() {
        val order = createOrder()

        coEvery {
            orderService.orderById(any())
        } returns order

        val resp = client.get()
            .uri("/v0.1/orders/${order.id}")
            .exchange()
            .expectStatus().isOk
            .expectBody(FlowOrderDto::class.java)
            .returnResult().responseBody!!

        resp shouldNotBe null
    }

    @Test
    fun `should return orders by ids - empty`() {
        coEvery {
            orderService.ordersByIds(any())
        } returns emptyFlow()

        client.post()
            .uri("/v0.1/orders/byIds")
            .bodyValue(FlowOrderIdsDto(emptyList()))
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `should return orders by ids`() {
        coEvery {
            orderService.ordersByIds(any())
        } returns (1L..10L).map { createOrder(it) }.asFlow()

        client.post()
            .uri("/v0.1/orders/byIds")
            .bodyValue(FlowOrderIdsDto((1L..10L).toList().map { it.toString() }))
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `should find all sell orders`() {
        coEvery {
            orderService.findAllSell(any(), any(), OrderFilter.Sort.LATEST_FIRST)
        } returns (1L..10L).map { createOrder(it) }.asFlow()

        client.shouldGetPaginatedResult<FlowOrdersPaginationDto>("/v0.1/orders/sell")
    }

    @Test
    fun `should find orders by collection - success`() {
        coEvery {
            orderService.getSellOrdersByCollection(eq("ABC"), any(), any(), OrderFilter.Sort.LATEST_FIRST)
        } returns (1L..10L).map { createOrder(it) }.asFlow()

        coEvery {
            orderService.getSellOrdersByCollection(neq("ABC"), any(), any(), OrderFilter.Sort.LATEST_FIRST)
        } returns emptyFlow()

        client.shouldGetPaginatedResult<FlowOrdersPaginationDto>(
            "/v0.1/orders/sell/byCollection?collection={collection}",
            "collection" to "ABC"
        )

        client.shouldGetPaginatedResult<FlowOrdersPaginationDto>(
            "/v0.1/orders/sell/byCollection?collection={collection}",
            "collection" to "DEF"
        )
    }

    @Test
    fun `should find orders by collection - no collection`() {
        client.shouldGetBadRequest("/v0.1/orders/sell/byCollection")
    }

    @Test
    fun `should find orders by item and status - success`() {
        coEvery {
            orderService.getSellOrdersByItemAndStatus(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                OrderFilter.Sort.AMOUNT_ASC
            )
        } returns (1L..10L).map { createOrder(it) }.asFlow()

        client.shouldGetPaginatedResult<FlowOrdersPaginationDto>(
            "/v0.1/orders/sell/byItemAndByStatus?contract={contract}&tokenId={tokenId}",
            mapOf<String, Any>(
                "contract" to "ABC",
                "tokenId" to 123L
            )
        )
    }

    @Test
    fun `should respond BAD_REQUEST by item and status - bad item id`() {
        client.shouldGetBadRequest(
            "/v0.1/orders/sell/byItemAndByStatus?contract={contract}&tokenId={tokenId}",
            mapOf(
                "contract" to "ABC", "tokenId" to "BAD"
            )
        )
    }

    @Test
    fun `should respond BAD_REQUEST by item and status - bad maker`() {
        client.shouldGetBadRequest(
            "/v0.1/orders/sell/byItemAndByStatus?contract={contract}&tokenId={tokenId}&maker={maker}",
            mapOf(
                "contract" to "ABC", "tokenId" to "BAD", "maker" to "NOT_FLOW_ADDRESS"
            )
        )
    }

    @Test
    fun `should respond BAD_REQUEST by item and status - bad currency`() {
        client.shouldGetBadRequest(
            "/v0.1/orders/sell/byItemAndByStatus?contract={contract}&tokenId={tokenId}&currency={currency}",
            mapOf(
                "contract" to "ABC", "tokenId" to "BAD", "currency" to "NOT_FLOW_ADDRESS"
            )
        )
    }

    @Test
    fun `should find orders by maker - success`() {
        coEvery {
            orderService.getSellOrdersByMaker(any(), any(), any(), OrderFilter.Sort.LATEST_FIRST)
        } returns (1L..10L).map { createOrder(it) }.asFlow()

        client.shouldGetPaginatedResult<FlowOrdersPaginationDto>(
            "/v0.1/orders/sell/byMaker?maker={maker}",
            mapOf<String, Any>(
                "maker" to "0x1337"
            )
        )
    }

    @Test
    fun `should respond BAD_REQUEST by maker - bad maker`() {
        client.shouldGetBadRequest(
            "/v0.1/orders/sell/byMaker?maker={maker}",
            mapOf(
                "maker" to "0xq337"
            )
        )

        client.shouldGetBadRequest(
            "/v0.1/orders/sell/byMaker"
        )
    }

    @Test
    fun `should sync orders`() {
        coEvery {
            orderService.findAll(any(), any(), OrderFilter.Sort.UPDATED_AT_DESC)
        } returns (1L..10L).map { createOrder(it) }.asFlow()

        client.shouldGetPaginatedResult<FlowOrdersPaginationDto>("/v0.1/orders/sync")
    }

    @Test
    fun `should find bids by item - success`() {
        coEvery {
            orderService.getBidOrdersByItem(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                OrderFilter.Sort.AMOUNT_DESC
            )
        } returns (1L..10L).map { createBidOrder(it) }.asFlow()

        val page = client.shouldGetPaginatedResult<FlowOrdersPaginationDto>(
            "/v0.1/bids/byItem?contract={contract}&tokenId={tokenId}&status=",
            mapOf<String, Any>(
                "contract" to "ABC",
                "tokenId" to 1337L
            )
        )

        page.items shouldHaveSize 10
    }

    @Test
    fun `should find bids by item with makers - success`() {
        coEvery {
            orderService.getBidOrdersByItem(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                OrderFilter.Sort.AMOUNT_DESC
            )
        } returns (1L..10L).map { createBidOrder(it) }.asFlow()

        val page = client.shouldGetPaginatedResult<FlowOrdersPaginationDto>(
            "/v0.1/bids/byItem?contract={contract}&tokenId={tokenId}&maker=0x01&maker=0x02&status=",
            mapOf<String, Any>(
                "contract" to "ABC",
                "tokenId" to 1337L
            )
        )

        page.items shouldHaveSize 10

        coVerify {
            orderService.getBidOrdersByItem(
                ItemId("ABC", 1337L),
                listOf(FlowAddress("0x01"), FlowAddress("0x02")),
                null,
                emptyList(),
                null,
                null,
                null,
                null,
                OrderFilter.Sort.AMOUNT_DESC
            )
        }
    }

    @Test
    fun `should find bids by item with makers - bad request - bad maker`() {
        coEvery {
            orderService.getBidOrdersByItem(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                OrderFilter.Sort.AMOUNT_DESC
            )
        } returns (1L..10L).map { createBidOrder(it) }.asFlow()

        client.shouldGetBadRequest(
            "/v0.1/bids/byItem?contract={contract}&tokenId={tokenId}&maker=0x0z&maker=0x02&status=",
            mapOf(
                *kotlin.arrayOf<kotlin.Pair<kotlin.String, kotlin.Any>>(
                    "contract" to "ABC",
                    "tokenId" to 1337L
                )
            )
        )
    }

    @Test
    fun `should find bid currencies`() {
        coEvery {
            orderService.bidCurrenciesByItemId(any())
        } returns flowOf(FlowAssetFungible("FLOW", BigDecimal.ZERO))

        client.get()
            .uri("/v0.1/bids/currencies/A.0b2a3299cc857e29.TopShot:1000")
            .exchange()
            .expectStatus().isOk

        client.shouldGetBadRequest("/v0.1/bids/currencies/A.0b2a3299cc857e29.TopShot+1000")
        client.shouldGetBadRequest("/v0.1/bids/currencies/A.0b2a3299cc857e29.TopShot:10T0")
    }

    private fun createOrder(tokenId: Long = randomLong()): Order {
        val itemId = ItemId("0x1a2b3c4d", tokenId)
        return Order(
            id = randomLong().toString(),
            itemId = itemId,
            maker = randomFlowAddress(),
            make = FlowAssetNFT(
                contract = itemId.contract,
                value = BigDecimal.valueOf(100L),
                tokenId = itemId.tokenId
            ),
            amount = BigDecimal.valueOf(100L),
            data = OrderData(
                payouts = listOf(Payout(randomFlowAddress(), BigDecimal.valueOf(1L))),
                originalFees = listOf(Payout(randomFlowAddress(), BigDecimal.valueOf(1L)))
            ),
            collection = "collection",
            take = FlowAssetFungible(
                "FLOW",
                BigDecimal.TEN
            ),
            makeStock = BigDecimal.TEN,
            lastUpdatedAt = LocalDateTime.now(ZoneOffset.UTC),
            createdAt = LocalDateTime.now(ZoneOffset.UTC),
            type = OrderType.LIST
        )
    }

    private fun createBidOrder(tokenId: Long = randomLong()): Order {
        val itemId = ItemId("0x1a2b3c4d", tokenId)
        return Order(
            id = randomLong().toString(),
            itemId = itemId,
            maker = randomFlowAddress(),
            make = FlowAssetFungible(
                "FLOW",
                BigDecimal.TEN
            ),
            amount = BigDecimal.valueOf(100L),
            data = OrderData(
                payouts = listOf(Payout(randomFlowAddress(), BigDecimal.valueOf(1L))),
                originalFees = listOf(Payout(randomFlowAddress(), BigDecimal.valueOf(1L)))
            ),
            collection = "collection",
            take = FlowAssetNFT(
                contract = itemId.contract,
                value = BigDecimal.valueOf(100L),
                tokenId = itemId.tokenId
            ),
            makeStock = BigDecimal.TEN,
            lastUpdatedAt = LocalDateTime.now(ZoneOffset.UTC),
            createdAt = LocalDateTime.now(ZoneOffset.UTC),
            type = OrderType.LIST
        )
    }
}
