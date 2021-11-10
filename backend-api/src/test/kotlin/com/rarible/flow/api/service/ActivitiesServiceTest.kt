package com.rarible.flow.api.service

import com.nftco.flow.sdk.FlowAddress
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.framework.model.Log
import com.rarible.flow.api.BaseIntegrationTest
import com.rarible.flow.api.IntegrationTest
import com.rarible.flow.core.domain.BurnActivity
import com.rarible.flow.core.domain.FlowActivityType
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.MintActivity
import com.rarible.flow.core.repository.ActivityContinuation
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.coSave
import com.rarible.protocol.dto.FlowBurnDto
import com.rarible.protocol.dto.FlowMintDto
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

@IntegrationTest
internal class ActivitiesServiceTest : BaseIntegrationTest() {

    @Autowired
    lateinit var activitiesService: ActivitiesService

    @Autowired
    lateinit var itemHistoryRepository: ItemHistoryRepository

    @BeforeEach
    fun beforeEach() {
        itemHistoryRepository.deleteAll().block()
    }

    @Test
    fun `should return activities byItem one by one`() = runBlocking<Unit> {

        val contract = "A.ebf4ae01d1284af8.RaribleNFT"
        val tokenId = 592L
        val mint = ItemHistory(
            Instant.parse("2021-11-09T17:35:19.947Z"),
            MintActivity(
                contract = contract, tokenId = tokenId, timestamp = Instant.now(),
                owner = FlowAddress("0x01").formatted, royalties = emptyList(), metadata = emptyMap()
            ),
            FlowLog("tx1", Log.Status.CONFIRMED, 1, "min", Instant.now(), 1, "block1")
        )
        val burn = ItemHistory(
            Instant.parse("2021-11-09T17:36:29.259Z"),
            BurnActivity(
                contract = contract, tokenId = tokenId, timestamp = Instant.now()
            ),
            FlowLog("tx2", Log.Status.CONFIRMED, 1, "burn", Instant.now(), 2, "block2")
        )
        itemHistoryRepository.coSave(mint)
        delay(10)
        itemHistoryRepository.coSave(burn)

        val one = activitiesService.getNftOrderActivitiesByItem(
            listOf(FlowActivityType.MINT, FlowActivityType.BURN),
            contract,
            tokenId,
            null,
            1,
            "LATEST_FIRST"
        )

        one.items shouldHaveSize 1
        one.items[0] should { it as FlowBurnDto }

        val two = activitiesService.getNftOrderActivitiesByItem(
            listOf(FlowActivityType.MINT, FlowActivityType.BURN),
            contract,
            tokenId,
            ActivityContinuation.of(one.continuation),
            1,
            "LATEST_FIRST"
        )

        two.items shouldHaveSize 1
        two.items[0] should { it as FlowMintDto }

    }

}