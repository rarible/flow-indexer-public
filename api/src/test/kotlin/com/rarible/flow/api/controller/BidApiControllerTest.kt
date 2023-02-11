package com.rarible.flow.api.controller

import com.nftco.flow.sdk.FlowAddress
import com.ninjasquad.springmockk.MockkBean
import com.rarible.flow.api.TestPropertiesConfiguration
import com.rarible.flow.api.http.shouldGetBadRequest
import com.rarible.flow.api.http.shouldGetPaginatedResult
import com.rarible.flow.api.service.OrderService
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.OrderFilter
import com.rarible.flow.randomFlowAddress
import com.rarible.flow.randomLong
import com.rarible.protocol.dto.FlowOrdersPaginationDto
import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.flow.asFlow
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
    controllers = [BidApiController::class],
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
class BidApiControllerTest {

    @MockkBean
    private lateinit var orderService: OrderService

    @Autowired
    lateinit var client: WebTestClient


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


    private fun createBidOrder(tokenId: Long = randomLong()): Order {
        val itemId = ItemId("0x1a2b3c4d", tokenId)
        val order = Order(
            id = randomLong(),
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
        return order
    }
}
