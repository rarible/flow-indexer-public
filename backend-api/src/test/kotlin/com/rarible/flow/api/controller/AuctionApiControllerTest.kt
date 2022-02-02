package com.rarible.flow.api.controller

import com.nftco.flow.sdk.FlowAddress
import com.ninjasquad.springmockk.MockkBean
import com.rarible.core.test.ext.MongoTest
import com.rarible.flow.api.TestPropertiesConfiguration
import com.rarible.flow.api.service.EnglishAuctionApiService
import com.rarible.flow.core.domain.AuctionStatus
import com.rarible.flow.core.domain.EnglishAuctionLot
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.protocol.dto.EnglishV1FlowAuctionDto
import io.mockk.coEvery
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.math.BigDecimal
import java.time.Instant

@WebFluxTest(
    controllers = [AuctionApiController::class],
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
@MongoTest
class AuctionApiControllerTest {

    @Autowired
    private lateinit var client: WebTestClient

    @MockkBean
    private lateinit var service: EnglishAuctionApiService

    @Test
    internal fun `should return auction by id`() {
        coEvery {
            service.byId(1L)
        } returns EnglishAuctionLot(
            id = 1L,
            status = AuctionStatus.ACTIVE,
            seller = FlowAddress("0x01"),
            sell = FlowAssetNFT(contract = "A.ebf4ae01d1284af8.RaribleNFT.NFT", tokenId = 1060L, value = BigDecimal.ONE),
            buyer = null,
            currency = "A.7e60df042a9c0868.FlowToken",
            createdAt = Instant.now(),
            lastUpdatedAt = Instant.now(),
            startAt = Instant.now(),
            startPrice = BigDecimal.ONE,
            minStep = BigDecimal.ONE
        )

        client.get().uri("/v0.1/auctions/{id}", 1L)
            .exchange().expectStatus().isOk
            .expectBody<EnglishV1FlowAuctionDto>()
            .consumeWith {
                val dto = it.responseBody
                Assertions.assertNotNull(dto, "Body must not be null!")
                Assertions.assertEquals(1L, dto!!.id)
                Assertions.assertNull(dto.duration)
            }
    }
}
