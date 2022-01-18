package com.rarible.flow.api.controller

import com.ninjasquad.springmockk.MockkBean
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.protocol.dto.FlowAggregationDataDto
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.flow.asFlow
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit

@WebFluxTest(
    controllers = [OrderAggregationController::class],
    properties = [
        "application.environment = test",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "spring.cloud.consul.config.enabled = false",
        "logging.logstash.tcp-socket.enabled = false",
    ]
)
@ActiveProfiles("test")
class OrderAggregationControllerTest(
    @Autowired val client: WebTestClient
) {

    @MockkBean
    lateinit var itemHistoryRepository: ItemHistoryRepository

    @Test
    fun `should aggregate NFT purchase by collection`() {
        every {
            itemHistoryRepository.aggregatePurchaseByCollection(
                any(), any(), any()
            )
        } returns listOf(
            FlowAggregationDataDto("0x01", BigDecimal.TEN, 12)
        ).asFlow()

        val start = Instant.now().minus(1, ChronoUnit.DAYS)
        val end = Instant.now()
        client
            .get()
            .uri(
                "/v0.1/aggregations/nftPurchaseByCollection?startDate={startDate}&endDate={endDate}&size=10",
                mapOf(
                    "startDate" to start.toEpochMilli(),
                    "endDate" to end.toEpochMilli()
                )
            )
            .exchange()
            .expectStatus().isOk

        coVerify {
            itemHistoryRepository.aggregatePurchaseByCollection(start, end, 10)
        }
    }

    @Test
    fun `should aggregate NFT purchase by taker`() {
        every {
            itemHistoryRepository.aggregatePurchaseByTaker(
                any(), any(), any()
            )
        } returns listOf(
            FlowAggregationDataDto("0x01", BigDecimal.TEN, 12)
        ).asFlow()

        val start = Instant.now().minus(1, ChronoUnit.DAYS)
        val end = Instant.now()

        client
            .get()
            .uri(
                "/v0.1/aggregations/nftPurchaseByTaker?startDate={startDate}&endDate={endDate}&size=10",
                mapOf(
                    "startDate" to start.toEpochMilli(),
                    "endDate" to end.toEpochMilli()
                )
            )
            .exchange()
            .expectStatus().isOk

        coVerify {
            itemHistoryRepository.aggregatePurchaseByTaker(start, end, 10)
        }
    }

    @Test
    fun `should aggregate NFT sell by maker`() {
        every {
            itemHistoryRepository.aggregateSellByMaker(
                any(), any(), any()
            )
        } returns listOf(
            FlowAggregationDataDto("0x01", BigDecimal.TEN, 12)
        ).asFlow()

        val start = Instant.now().minus(1, ChronoUnit.DAYS)
        val end = Instant.now()
        client
            .get()
            .uri(
                "/v0.1/aggregations/nftSellByMaker?startDate={startDate}&endDate={endDate}&size=10",
                mapOf(
                    "startDate" to start.toEpochMilli(),
                    "endDate" to end.toEpochMilli()
                )
            )
            .exchange()
            .expectStatus().isOk

        coVerify {
            itemHistoryRepository.aggregateSellByMaker(start, end, 10)
        }
    }

}
