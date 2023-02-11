package com.rarible.flow.api.service

import com.nftco.flow.sdk.FlowAddress
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.core.kafka.KafkaSendResult
import com.rarible.flow.api.IntegrationTest
import com.rarible.flow.api.createOrder
import com.rarible.flow.core.domain.FlowActivityType
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.MintActivity
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.Part
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.ItemMetaRepository
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.core.repository.coSaveAll
import com.rarible.flow.randomAddress
import com.rarible.flow.randomFlowAddress
import com.rarible.flow.randomLong
import com.rarible.flow.randomRate
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.random.Random

@IntegrationTest
internal class AdminServiceTest {

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var ownershipRepository: OwnershipRepository

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var itemMetaRepository: ItemMetaRepository

    @Autowired
    private lateinit var itemHistoryRepository: ItemHistoryRepository

    private lateinit var adminService: AdminService

    @BeforeEach
    internal fun setUp(): Unit = runBlocking {
        itemRepository.deleteAll().awaitFirstOrNull()

        val protocolEventPublisher = mockk<ProtocolEventPublisher> {
            coEvery { onItemDelete(any()) } answers { KafkaSendResult.Success(firstArg<ItemId>().toString()) }
            coEvery { onDelete(any<List<Ownership>>()) } answers { KafkaSendResult.Success(firstArg<List<Ownership>>().size.toString()) }
        }
        adminService = AdminService(
            itemRepository,
            itemMetaRepository,
            ownershipRepository,
            protocolEventPublisher,
            orderRepository,
            itemHistoryRepository
        )
    }

    @Test
    fun deleteItemById(): Unit = runBlocking {

        val now = Instant.now(Clock.systemUTC())
        val item = Item(
            contract = "A.${randomAddress()}.RaribleNFT.NFT",
            tokenId = randomLong(),
            creator = FlowAddress(randomAddress()),
            royalties = listOf(),
            owner = FlowAddress(randomAddress()),
            mintedAt = now,
            meta = randomAddress(),
            collection = randomAddress(),
            updatedAt = now
        )

        itemRepository.save(item).awaitFirstOrNull()

        val owner = FlowAddress(randomAddress())

        val all = listOf(
            createOwnership(item, owner),
            createOwnership(item, owner),
            createOwnership(item, owner)
        )
            .sortedBy { it.id.toString() }
        ownershipRepository.coSaveAll(all)

        val order = createOrder().copy(itemId = item.id)
        orderRepository.coSave(order)

        val itemHistory = ItemHistory(
            activity = randomMint(item),
            log = randomLog(),
            date = Instant.now()
        )
        itemHistoryRepository.coSave(itemHistory)

        adminService.deleteItemById(item.id)

        val item1 = itemRepository.findById(item.id).awaitFirstOrNull()
        assertNull(item1)

        val ownerships =
            ownershipRepository.findAllByContractAndTokenId(item.contract, item.tokenId).collectList().awaitFirst()
        assertTrue(ownerships.isEmpty())

        val orders = orderRepository.findByItemId(item.id).collectList().awaitFirst()
        assertTrue(orders.isEmpty())

        val itemHistories = itemHistoryRepository.findByItemId(item.contract, item.tokenId).collectList().awaitFirst()
        assertTrue(itemHistories.isEmpty())

    }

    private fun createOwnership(item: Item, owner: FlowAddress) =
        Ownership(
            item.contract,
            Random.nextLong(),
            owner,
            owner,
            Instant.now().truncatedTo(ChronoUnit.SECONDS),
        )

    private fun randomMint(item: Item) = MintActivity(
        type = FlowActivityType.MINT,
        timestamp = Instant.now(Clock.systemUTC()),
        owner = randomAddress(),
        creator = randomAddress(),
        contract = item.contract,
        tokenId = item.tokenId,
        value = 1,
        metadata = mapOf("metaURI" to "ipfs://"),
        royalties = (0..Random.Default.nextInt(0, 3)).map { Part(randomFlowAddress(), randomRate()) },
    )

    private fun randomLog() =
        FlowLog(
            transactionHash = UUID.randomUUID().toString(),
            eventIndex = 1,
            eventType = "",
            timestamp = Instant.now(Clock.systemUTC()),
            blockHeight = randomLong(),
            blockHash = ""
        )
}