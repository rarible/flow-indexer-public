package com.rarible.flow.api.controller

import com.ninjasquad.springmockk.MockkBean
import com.rarible.flow.api.TestPropertiesConfiguration
import com.rarible.flow.api.service.OrderService
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.OrderFilter
import com.rarible.flow.randomFlowAddress
import com.rarible.flow.randomLong
import com.rarible.protocol.dto.FlowOrderDto
import com.rarible.protocol.dto.FlowOrderIdsDto
import com.rarible.protocol.dto.FlowOrdersPaginationDto
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDateTime
import java.time.ZoneOffset

@WebFluxTest(
    controllers = [OrderApiController::class],
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
class OrderApiControllerTest {

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
            .bodyValue(FlowOrderIdsDto((1L..10L).toList()))
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `should find all sell orders`() {
        coEvery {
            orderService.findAll(any(), any(), OrderFilter.Sort.LATEST_FIRST)
        } returns (1L..10L).map { createOrder(it) }.asFlow()

        shouldGetPaginatedResult("/v0.1/orders/sell")
    }

    @Test
    fun `should find orders by collection - success`() {
        coEvery {
            orderService.getSellOrdersByCollection(eq("ABC"), any(), any(), OrderFilter.Sort.LATEST_FIRST)
        } returns (1L..10L).map { createOrder(it) }.asFlow()

        coEvery {
            orderService.getSellOrdersByCollection(neq("ABC"), any(), any(), OrderFilter.Sort.LATEST_FIRST)
        } returns emptyFlow()

        shouldGetPaginatedResult("/v0.1/orders/sell/byCollection?collection={collection}",
            "collection" to "ABC"
        )

        shouldGetPaginatedResult("/v0.1/orders/sell/byCollection?collection={collection}",
            "collection" to "DEF"
        )
    }

    @Test
    fun `should find orders by collection - no collection`() {
        shouldGetBadRequest("/v0.1/orders/sell/byCollection")
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
                OrderFilter.Sort.MAKE_PRICE_ASC
            )
        } returns (1L..10L).map { createOrder(it) }.asFlow()

        shouldGetPaginatedResult(
            "/v0.1/orders/sell/byItemAndByStatus?contract={contract}&tokenId={tokenId}",
            "contract" to "ABC",
            "tokenId" to 123L
        )
    }

    @Test
    fun `should respond BAD_REQUEST by item and status - bad item id`() {
        shouldGetBadRequest("/v0.1/orders/sell/byItemAndByStatus?contract={contract}&tokenId={tokenId}",
            "contract" to "ABC",
            "tokenId" to "BAD"
        )
    }

    @Test
    fun `should respond BAD_REQUEST by item and status - bad maker`() {
        shouldGetBadRequest(
            "/v0.1/orders/sell/byItemAndByStatus?contract={contract}&tokenId={tokenId}&maker={maker}",
            "contract" to "ABC",
            "tokenId" to "BAD",
            "maker" to "NOT_FLOW_ADDRESS"
        )
    }

    @Test
    fun `should respond BAD_REQUEST by item and status - bad currency`() {
        shouldGetBadRequest(
            "/v0.1/orders/sell/byItemAndByStatus?contract={contract}&tokenId={tokenId}&currency={currency}",
            "contract" to "ABC",
            "tokenId" to "BAD",
            "currency" to "NOT_FLOW_ADDRESS"
        )
    }

    @Test
    fun `should find orders by maker - success`() {
        coEvery {
            orderService.getSellOrdersByMaker(any(), any(), any(), any(), OrderFilter.Sort.LATEST_FIRST)
        } returns (1L..10L).map { createOrder(it) }.asFlow()

        shouldGetPaginatedResult(
            "/v0.1/orders/sell/byMaker?maker={maker}",
            "maker" to "0x1337"
        )
    }

    @Test
    fun `should respond BAD_REQUEST by maker - bad maker`() {
        shouldGetBadRequest(
            "/v0.1/orders/sell/byMaker?maker={maker}",
            "maker" to "0xq337"
        )

        shouldGetBadRequest(
            "/v0.1/orders/sell/byMaker"
        )
    }

    @Test
    fun `should find orders bids by item - success`() {
        val page = shouldGetPaginatedResult(
            "/v0.1/orders/bids/byItem?contract={contract}&tokenId={tokenId}",
            "contract" to "ABC",
            "tokenId" to 1337L
        )

        page.items shouldHaveSize 0 // for now we return empty bids list
    }

    @Test
    fun `should find bids by item - success`() {
        val page = shouldGetPaginatedResult(
            "/v0.1/bids/byItem?contract={contract}&tokenId={tokenId}&status=",
            "contract" to "ABC",
            "tokenId" to 1337L
        )

        page.items shouldHaveSize 0 // for now we return empty bids list
    }

    private fun shouldGetPaginatedResult(url: String, params: Map<String, Any> = emptyMap()): FlowOrdersPaginationDto {
        return client.get()
            .uri(url, params)
            .exchange()
            .expectStatus().isOk
            .expectBody(FlowOrdersPaginationDto::class.java)
            .returnResult().responseBody!!
    }

    private fun shouldGetPaginatedResult(url: String, vararg params: Pair<String, Any>): FlowOrdersPaginationDto {
        return shouldGetPaginatedResult(url, mapOf(*params))
    }

    private fun shouldGetBadRequest(url: String, params: Map<String, Any> = emptyMap()) {
        client.get()
            .uri(url, params)
            .exchange()
            .expectStatus().isBadRequest
    }

    private fun shouldGetBadRequest(url: String, vararg params: Pair<String, Any>) {
        shouldGetBadRequest(url, mapOf(*params))
    }

    private fun createOrder(tokenId: Long = randomLong()): Order {
        val itemId = ItemId("0x1a2b3c4d", tokenId)
        val order = Order(
            id = randomLong(),
            itemId = itemId,
            maker = randomFlowAddress(),
            make = FlowAssetNFT(
                contract = itemId.contract,
                value = BigDecimal.valueOf(100L),
                tokenId = itemId.tokenId
            ),
            amount = BigDecimal.valueOf(100L),
//            amountUsd = BigDecimal.valueOf(100L),
            data = OrderData(
                payouts = listOf(Payout(randomFlowAddress(), BigDecimal.valueOf(1L))),
                originalFees = listOf(Payout(randomFlowAddress(), BigDecimal.valueOf(1L)))
            ),
            collection = "collection",
            take = FlowAssetFungible(
                "FLOW",
                BigDecimal.TEN
            ),
            makeStock = BigInteger.TEN,
            lastUpdatedAt = LocalDateTime.now(ZoneOffset.UTC),
            createdAt = LocalDateTime.now(ZoneOffset.UTC),
            type = OrderType.LIST
        )
        return order
    }
}
