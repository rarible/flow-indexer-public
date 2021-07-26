package com.rarible.flow.api.controller

import com.rarible.flow.core.domain.BurnActivity
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.MintActivity
import com.rarible.flow.core.domain.TransferActivity
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.protocol.dto.FlowNftActivityDto
import com.rarible.protocol.dto.MintDto
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.onflow.sdk.FlowAddress
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBodyList
import java.time.LocalDateTime
import java.util.*

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
class NftOrderActivityControllerTest {

    @Autowired
    private lateinit var repo: ItemHistoryRepository

    @Autowired
    lateinit var client: WebTestClient

    private val testAddress = "0x5c075acc71f2f41c"

    @BeforeEach
    internal fun setUp() {
        repo.deleteAll().block()
    }

    @Test
    fun `should return 1 activity`() {
        val mintActivity = MintActivity(
            owner = FlowAddress(testAddress),
            contract = FlowAddress(testAddress),
            tokenId = 1L,
            value = 1L,
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = 1000L,
        )

        val transferActivity = TransferActivity(
            from = FlowAddress(testAddress),
            owner = FlowAddress(testAddress),
            contract = FlowAddress(testAddress),
            tokenId = 1L,
            value = 1L,
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = 1001L,
        )

        val burnActivity = BurnActivity(
            owner = FlowAddress(testAddress),
            contract = FlowAddress(testAddress),
            tokenId = 1L,
            value = 1L,
            transactionHash = UUID.randomUUID().toString(),
            blockHash = UUID.randomUUID().toString(),
            blockNumber = 1002L,
        )

        val history = listOf(
            ItemHistory(id = UUID.randomUUID().toString(), date = LocalDateTime.now(), mintActivity),
            ItemHistory(id = UUID.randomUUID().toString(), date = LocalDateTime.now(), mintActivity.copy(tokenId = 2L)),
            ItemHistory(id = UUID.randomUUID().toString(), date = LocalDateTime.now(), transferActivity),
            ItemHistory(id = UUID.randomUUID().toString(), date = LocalDateTime.now(), burnActivity),
        )

        repo.saveAll(history).then().block()

        val activities = client.get()
            .uri("/v0.1/activities/byItem?type=MINT&contract=${testAddress}&tokenId=1")
            .exchange()
            .expectStatus().isOk
            .expectBodyList<FlowNftActivityDto>().hasSize(1)
            .returnResult().responseBody!!

        Assertions.assertNotNull(activities[0])

        Assertions.assertTrue(activities[0] is MintDto, "Wrong DTO type!")

    }
}
