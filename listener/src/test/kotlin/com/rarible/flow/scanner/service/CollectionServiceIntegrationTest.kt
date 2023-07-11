package com.rarible.flow.scanner.service

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.UInt64NumberField
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.core.task.Task
import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskStatus
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.MintActivity
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.OwnershipId
import com.rarible.flow.core.event.EventId
import com.rarible.flow.core.event.EventMessage
import com.rarible.flow.core.repository.FlowLogEventRepository
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.core.test.Data
import com.rarible.flow.scanner.test.AbstractIntegrationTest
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.util.Date

internal class CollectionServiceIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var collectionService: CollectionService

    @Autowired
    lateinit var taskRepository: TaskRepository

    @Autowired
    lateinit var flowLogEventRepository: FlowLogEventRepository

    @Autowired
    lateinit var itemHistoryRepository: ItemHistoryRepository

    @Autowired
    lateinit var itemRepository: ItemRepository

    @Autowired
    lateinit var ownershipRepository: OwnershipRepository

    val eventType = "${Contracts.KICKS.fqn(FlowChainId.MAINNET)}.SneakerCreated"
    val flowLog = FlowLog(
        "tx",  0, eventType, Instant.now(), 1000, "block"
    )

    @Test
    fun `should restart descriptor`(): Unit = runBlocking {
        taskRepository.save(Task(
            type = "RECONCILIATION",
            param = "StarlyCardDescriptor",
            state = 23778634,
            running = false,
            lastStatus = TaskStatus.COMPLETED,
            lastFinishDate = Date(),
            lastUpdateDate = Date(),
            sample = 5000
        )).awaitSingle()

        collectionService.restartDescriptor(Contracts.STARLY_CARD, 18133134L)

        taskRepository.findAll().awaitFirstOrNull() should {
            it as Task
            it.state shouldBe 18133134L
            it.lastStatus shouldBe TaskStatus.NONE
        }
    }

    @Test
    fun `should purge flow_log_event`(): Unit = runBlocking {
        flowLogEventRepository.saveAll( listOf(
                FlowLogEvent(
                    log = flowLog,
                    "id01",
                    EventMessage(
                        EventId.of(eventType), mapOf(
                            "id" to UInt64NumberField("42")
                        )
                    ),
                    FlowLogType.MINT
                ),
                FlowLogEvent(
                    log = flowLog.copy(eventIndex = 1),
                    "id02",
                    EventMessage(
                        EventId.of("A.1234.MotoGP.Deposit"), mapOf(
                            "id" to UInt64NumberField("42")
                        )
                    ),
                    FlowLogType.DEPOSIT
                )
            )
        ).awaitLast()

        collectionService.purgeLogEvents(Contracts.KICKS, FlowChainId.MAINNET)

        flowLogEventRepository.count().awaitSingle() shouldBe 1
    }

    @Test
    fun `should purge item_history`(): Unit = runBlocking {
        itemHistoryRepository.save(
            ItemHistory(
                Instant.now(),
                MintActivity(
                    owner = "0x01",
                    contract = Contracts.KICKS.fqn(FlowChainId.MAINNET),
                    tokenId = 1000,
                    timestamp = Instant.now(),
                    creator = "0x01",
                    royalties = emptyList(),
                    metadata = emptyMap()
                ),
                flowLog
            )
        ).awaitSingle()

        collectionService.purgeItemHistory(Contracts.KICKS, FlowChainId.MAINNET)

        itemHistoryRepository.count().awaitSingle() shouldBe 0
    }

    @Test
    fun `should purge items`(): Unit = runBlocking {
        val contract = Contracts.JAMBB_MOMENTS.fqn(FlowChainId.MAINNET)
        itemRepository.saveAll(
            listOf(
                Data.createItem().copy(contract = contract),
                Data.createItem().copy(contract = contract, tokenId = 13),
                Data.createItem()
            )
        ).awaitLast()

        collectionService.purgeItems(Contracts.JAMBB_MOMENTS, FlowChainId.MAINNET)

        itemRepository.count().awaitSingle() shouldBe 1
    }

    @Test
    fun `should purge ownerships`(): Unit = runBlocking {
        val contract = Contracts.JAMBB_MOMENTS.fqn(FlowChainId.MAINNET)
        ownershipRepository.saveAll(
            listOf(
                Ownership(OwnershipId(contract, 1, FlowAddress("0x01")), FlowAddress("0x0a")),
                Ownership(OwnershipId(contract, 2, FlowAddress("0x01")), FlowAddress("0x0a")),
                Ownership(OwnershipId(Contracts.CHAINMONSTERS.fqn(FlowChainId.MAINNET), 1, FlowAddress("0x02")), FlowAddress("0x0a"))
            )
        ).awaitLast()

        collectionService.purgeOwnerships(Contracts.JAMBB_MOMENTS, FlowChainId.MAINNET)

        ownershipRepository.count().awaitSingle() shouldBe 1
    }

}