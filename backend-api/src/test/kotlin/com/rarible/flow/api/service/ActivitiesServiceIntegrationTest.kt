package com.rarible.flow.api.service

import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.framework.model.Log
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomString
import com.rarible.flow.api.BaseIntegrationTest
import com.rarible.flow.api.IntegrationTest
import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.MintActivity
import com.rarible.flow.core.domain.Part
import com.rarible.flow.core.domain.TransferActivity
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.coSaveAll
import com.rarible.flow.randomAddress
import com.rarible.flow.randomContract
import com.rarible.flow.randomHash
import com.rarible.flow.randomLong
import com.rarible.protocol.dto.FlowMintDto
import com.rarible.protocol.dto.FlowTransferDto
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

@FlowPreview
@IntegrationTest
internal class ActivitiesServiceIntegrationTest: BaseIntegrationTest() {

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    lateinit var itemHistoryRepository: ItemHistoryRepository

    @Autowired
    lateinit var activitiesService: ActivitiesService

    @Test
    fun `should return correct continuation FB-646 - fix`(): Unit = runBlocking {

        val activity1 = ItemHistory(
            Instant.parse("2021-02-25T05:39:22.00Z"),
            MintActivity(
                owner = "0x0b2a3299cc857e29",
                creator = "0x0b2a3299cc857e29",
                contract = "A.0b2a3299cc857e29.TopShot",
                tokenId = 3547597,
                value = 1,
                timestamp = Instant.parse("2021-02-25T05:39:22.00Z"),
                royalties = emptyList(),
                metadata = emptyMap()
            ),
            FlowLog(
                transactionHash = "1903e78154fb70a7fd410e980fa6aaa07199226c5855e743d73b85cd19dcfd62",
                status = Log.Status.CONFIRMED,
                eventIndex = 0,
                eventType = "A.0b2a3299cc857e29.TopShot.MomentMinted",
                timestamp = Instant.ofEpochMilli(1614231562934000),
                blockHeight = 12236741,
                blockHash = "0954c38a1189717a26fe16afce2f06c257dee03e2224496be5aa01b59545c7d0",
            )
        )

        val activity2 = ItemHistory(
            Instant.parse("2021-02-25T05:39:22.00Z"),
            MintActivity(
                owner = "0x0b2a3299cc857e29",
                creator = "0x0b2a3299cc857e29",
                contract = "A.0b2a3299cc857e29.TopShot",
                tokenId = 3547598,
                value = 1,
                timestamp = Instant.parse("2021-02-25T05:39:22.00Z"),
                royalties = emptyList(),
                metadata = emptyMap()
            ),
            FlowLog(
                transactionHash = "1903e78154fb70a7fd410e980fa6aaa07199226c5855e743d73b85cd19dcfd62",
                status = Log.Status.CONFIRMED,
                eventIndex = 1,
                eventType = "A.0b2a3299cc857e29.TopShot.MomentMinted",
                timestamp = Instant.ofEpochMilli(1614231562934000),
                blockHeight = 12236741,
                blockHash = "0954c38a1189717a26fe16afce2f06c257dee03e2224496be5aa01b59545c7d0",
            )
        )


        itemHistoryRepository.coSaveAll(activity1, activity2)
        val sort = "EARLIEST_FIRST"
        val cursor1 = "1614231562000_1903e78154fb70a7fd410e980fa6aaa07199226c5855e743d73b85cd19dcfd62.0"
        val cursor2 = "1614231562000_1903e78154fb70a7fd410e980fa6aaa07199226c5855e743d73b85cd19dcfd62.1"
        val types = listOf(
            "MINT", "BURN", "TRANSFER"
        )
        val res1 = activitiesService.getNftOrderAllActivities(types, null, 1, sort)
        res1.continuation shouldBe cursor1
        res1.items.count() shouldBe 1

        val res2 = activitiesService.getNftOrderAllActivities(types, res1.continuation, 1, sort)


        res2.continuation shouldBe cursor2
        res2.items.count() shouldBe 1

        val res3 = activitiesService.getNftOrderAllActivities(types, res2.continuation, 1, sort)

        res3.continuation shouldBe "null"
        res3.items.isEmpty() shouldBe true

    }

    @Test
    fun `should find activities by ids`(): Unit = runBlocking {
        val activity1 = ItemHistory(
            Instant.parse("2021-02-25T05:39:22.00Z"),
            MintActivity(
                owner = "0x0b2a3299cc857e29",
                creator = "0x0b2a3299cc857e29",
                contract = "A.0b2a3299cc857e29.TopShot",
                tokenId = 3547597,
                value = 1,
                timestamp = Instant.parse("2021-02-25T05:39:22.00Z"),
                royalties = emptyList(),
                metadata = emptyMap()
            ),
            FlowLog(
                transactionHash = "1903e78154fb70a7fd410e980fa6aaa07199226c5855e743d73b85cd19dcfd62",
                status = Log.Status.CONFIRMED,
                eventIndex = 0,
                eventType = "A.0b2a3299cc857e29.TopShot.MomentMinted",
                timestamp = Instant.ofEpochMilli(1614231562934000),
                blockHeight = 12236741,
                blockHash = "0954c38a1189717a26fe16afce2f06c257dee03e2224496be5aa01b59545c7d0",
            )
        )

        val activity2 = ItemHistory(
            Instant.parse("2021-02-25T05:39:22.00Z"),
            MintActivity(
                owner = "0x0b2a3299cc857e29",
                creator = "0x0b2a3299cc857e29",
                contract = "A.0b2a3299cc857e29.TopShot",
                tokenId = 3547598,
                value = 1,
                timestamp = Instant.parse("2021-02-25T05:39:22.00Z"),
                royalties = emptyList(),
                metadata = emptyMap()
            ),
            FlowLog(
                transactionHash = "1903e78154fb70a7fd410e980fa6aaa07199226c5855e743d73b85cd19dcfd62",
                status = Log.Status.CONFIRMED,
                eventIndex = 1,
                eventType = "A.0b2a3299cc857e29.TopShot.MomentMinted",
                timestamp = Instant.ofEpochMilli(1614231562934000),
                blockHeight = 12236741,
                blockHash = "0954c38a1189717a26fe16afce2f06c257dee03e2224496be5aa01b59545c7d0",
            )
        )


        itemHistoryRepository.coSaveAll(activity1, activity2)
        activitiesService.getActivitiesByIds(listOf(activity1.id)).items.map { it.id } shouldContainExactly listOf(
            activity1.id
        )
        activitiesService.getActivitiesByIds(listOf(activity2.id)).items.map { it.id } shouldContainExactly listOf(
            activity2.id
        )
    }

    @Test
    fun `should find activities by item and owner`() = runBlocking<Unit> {
        val sort = "LATEST_FIRST"
        val owner = randomAddress()
        val contract = "A.0b2a3299cc857e29.TopShot"
        val tokenId = randomLong()
        val timestamp = Instant.now()

        val mint = randomMint(
            owner = owner,
            contract = contract,
            tokenId = tokenId,
            timestamp = timestamp
        )
        val transfer = randomTransfer(
            to = owner,
            contract = contract,
            tokenId = tokenId,
            timestamp = timestamp.minusMillis(1000)
        )
        itemHistoryRepository.coSaveAll(listOf(mint, transfer).map { randomItemHistory(it) })

        activitiesService
            .getNftOrderActivitiesByItemAndOwner(contract, tokenId, owner, null, null, sort)
            ?.let { activities ->
                activities.items shouldHaveSize 2
                activities.items[0] should { it is FlowMintDto }
                activities.items[1] should { it is FlowTransferDto }
            } ?: shouldNotBe(null)
    }

    fun randomItemHistory(
        activity: BaseActivity,
        log: FlowLog = randomFlowLog(),
    ) = ItemHistory(
        date = activity.timestamp,
        activity = activity,
        log = log,
    )

    fun randomFlowLog(
        transactionHash: String = randomHash(),
        status: Log.Status = Log.Status.CONFIRMED,
        eventIndex: Int = randomInt(100),
        eventType: String = randomString(),
        timestamp: Instant = Instant.now(),
        blockHeight: Long = randomLong(),
        blockHash: String = randomHash(),
    ) = FlowLog(
        transactionHash = transactionHash,
        status = status,
        eventIndex = eventIndex,
        eventType = eventType,
        timestamp = timestamp,
        blockHeight = blockHeight,
        blockHash = blockHash,
    )

    fun randomMint(
        owner: String = randomAddress(),
        creator: String = randomAddress(),
        contract: String = randomContract(),
        tokenId: Long = randomLong(),
        value: Long = 1L,
        timestamp: Instant = Instant.now().minusMillis(com.rarible.core.test.data.randomLong(100000)),
        royalties: List<Part> = emptyList(),
        metadata: Map<String, String> = emptyMap(),
    ) = MintActivity(
        owner = owner,
        contract = contract,
        tokenId = tokenId,
        value = value,
        timestamp = timestamp,
        creator = creator,
        royalties = royalties,
        metadata = metadata
    )

    fun randomTransfer(
        contract: String = randomContract(),
        tokenId: Long = randomLong(),
        timestamp: Instant = Instant.now().minusMillis(com.rarible.core.test.data.randomLong(100000)),
        from: String = randomAddress(),
        to: String = randomAddress(),
    ) = TransferActivity(
        contract = contract,
        tokenId = tokenId,
        timestamp = timestamp,
        from = from,
        to = to,
    )
}
