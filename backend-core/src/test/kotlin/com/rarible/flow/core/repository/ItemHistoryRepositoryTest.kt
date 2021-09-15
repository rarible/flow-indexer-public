package com.rarible.flow.core.repository

import com.nftco.flow.sdk.FlowAddress
import com.rarible.core.test.ext.MongoTest
import com.rarible.flow.core.config.CoreConfig
import com.rarible.flow.core.domain.*
import io.kotest.matchers.collections.shouldHaveSize
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@MongoTest
@DataMongoTest(
    properties = [
        "application.environment = dev",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "spring.cloud.consul.config.enabled = false",
        "logging.logstash.tcp-socket.enabled = false",
        "spring.data.mongodb.auto-index-creation = true"
    ]
)
@ContextConfiguration(classes = [CoreConfig::class])
@ActiveProfiles("test")
@Disabled
class ItemHistoryRepositoryTest {

    @Autowired
    private lateinit var itemHistoryRepository: ItemHistoryRepository

    @BeforeEach
    fun setUp() {
        itemHistoryRepository.deleteAll().block()
        val sell = FlowNftOrderActivitySell(
            price = BigDecimal.ONE,
            collection = "c1",
            tokenId = 1,
            left = OrderActivityMatchSide(
                FlowAddress("0x01"),
                FlowAssetNFT("c1", BigDecimal.ONE, 1)
            ),
            right = OrderActivityMatchSide(
                FlowAddress("0x02"),
                FlowAssetFungible("flow", BigDecimal.ONE)
            ),
            transactionHash = "txhash",
            blockHash = "blockhash",
            blockNumber = 1000,
            contract = "Contract"
        )

        val events = listOf(
            ItemHistory(UUID.randomUUID().toString(), Instant.now(), sell),
            ItemHistory(UUID.randomUUID().toString(), Instant.now(), sell.copy(
                price = BigDecimal.valueOf(10L),
                tokenId = 2,
                left = OrderActivityMatchSide(
                    FlowAddress("0x01"),
                    FlowAssetNFT("c1", BigDecimal.valueOf(10L), 2)
                ),
                right = OrderActivityMatchSide(
                    FlowAddress("0x02"),
                    FlowAssetFungible("flow", BigDecimal.valueOf(10L))
                )
            )),
            ItemHistory(UUID.randomUUID().toString(), Instant.now(), sell.copy(
                price = BigDecimal.valueOf(100L),
                tokenId = 3,
                left = OrderActivityMatchSide(
                    FlowAddress("0x02"),
                    FlowAssetNFT("c2", BigDecimal.valueOf(100L), 2)
                ),
                right = OrderActivityMatchSide(
                    FlowAddress("0x03"),
                    FlowAssetFungible("flow", BigDecimal.valueOf(100L))
                )
            )),
        )

        itemHistoryRepository.saveAll(events).blockLast()
    }

    @Test
    fun `should aggregate by collection`() = runBlocking<Unit> {
        val aggregations = itemHistoryRepository.aggregatePurchaseByCollection(
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(1, ChronoUnit.DAYS),
            null
        ).toList()

        aggregations shouldHaveSize 2
    }

    @Test
    fun `should aggregate purchase by taker`() = runBlocking<Unit> {
        val aggregations = itemHistoryRepository.aggregatePurchaseByTaker(
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(1, ChronoUnit.DAYS),
            null
        ).toList()

        aggregations shouldHaveSize 2
    }

    @Test
    fun `should aggregate sell by maker`() = runBlocking<Unit> {
        val aggregations = itemHistoryRepository.aggregateSellByMaker(
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(1, ChronoUnit.DAYS),
            null
        ).toList()

        aggregations shouldHaveSize 2
    }
}


