package com.rarible.flow.core.repository

import com.nftco.flow.sdk.FlowAddress
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.framework.model.Log
import com.rarible.core.test.ext.MongoTest
import com.rarible.flow.core.config.CoreConfig
import com.rarible.flow.core.domain.FlowAssetFungible
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.OrderActivityMatchSide
import com.rarible.flow.core.repository.data.createSellActivity
import com.rarible.protocol.dto.FlowAggregationDataDto
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit

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
class ItemHistoryAggregationsTest {

    @Autowired
    private lateinit var itemHistoryRepository: ItemHistoryRepository

    @BeforeEach
    fun setUp() {
        itemHistoryRepository.deleteAll().block()
        val sell = createSellActivity()

        val log = FlowLog(
            "txHash", Log.Status.CONFIRMED, 1, "abc", Instant.now(), 1L, "blockHash"
        )

        val events = listOf(
            ItemHistory(Instant.now(), sell, log),
            ItemHistory(Instant.now(), sell.copy(
                price = BigDecimal.valueOf(10L),
                priceUsd = BigDecimal("2.7"),
                tokenId = 2,
                left = OrderActivityMatchSide(
                    FlowAddress("0x01").formatted,
                    FlowAssetNFT("c1", BigDecimal.valueOf(10L), 2)
                ),
                right = OrderActivityMatchSide(
                    FlowAddress("0x03").formatted,
                    FlowAssetFungible("flow", BigDecimal.valueOf(13L))
                )
            ), log.copy(eventIndex = 2)),
            ItemHistory(
                Instant.now(), sell.copy(
                contract = "c2",
                price = BigDecimal.valueOf(100L),
                priceUsd = BigDecimal("4.83"),
                tokenId = 3,
                left = OrderActivityMatchSide(
                    FlowAddress("0x02").formatted,
                    FlowAssetNFT("c2", BigDecimal.valueOf(100L), 2)
                ),
                right = OrderActivityMatchSide(
                    FlowAddress("0x03").formatted,
                    FlowAssetFungible("flow", BigDecimal.valueOf(100L))
                )
            ), log.copy(eventIndex = 3)),
        )

        itemHistoryRepository.saveAll(events).blockLast()
    }

    @Test
    fun `should find all entries`() {
        runBlocking {
            itemHistoryRepository
                .coFindAll()
                .count() shouldBe 3
        }
    }

    @Test
    fun `should aggregate by collection`() {
        runBlocking {
            val aggregations = itemHistoryRepository.aggregatePurchaseByCollection(
                Instant.now().minus(1, ChronoUnit.DAYS),
                Instant.now().plus(1, ChronoUnit.DAYS),
                null
            ).toList()

            aggregations shouldHaveSize 2
            aggregations shouldContainAll listOf(
                FlowAggregationDataDto("c1", BigDecimal("3.8"), 2),
                FlowAggregationDataDto("c2", BigDecimal("4.83"), 1)
            )
        }
    }

    @Test
    fun `should aggregate purchase by taker`() = runBlocking {
        val aggregations = itemHistoryRepository.aggregatePurchaseByTaker(
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(1, ChronoUnit.DAYS),
            null
        ).toList()

        aggregations shouldHaveSize 2
        aggregations shouldContainAll listOf(
            FlowAggregationDataDto(FlowAddress("0x02").formatted, BigDecimal("1.1"), 1),
            FlowAggregationDataDto(FlowAddress("0x03").formatted, BigDecimal("7.53"), 2)
        )

    }

    @Test
    fun `should aggregate sell by maker`() = runBlocking<Unit> {
        val aggregations = itemHistoryRepository.aggregateSellByMaker(
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(1, ChronoUnit.DAYS),
            null
        ).toList()

        aggregations shouldHaveSize 2
        aggregations shouldContainAll listOf(
            FlowAggregationDataDto(FlowAddress("0x01").formatted, BigDecimal("3.8"), 2),
            FlowAggregationDataDto(FlowAddress("0x02").formatted, BigDecimal("4.83"), 1)
        )
    }
}


