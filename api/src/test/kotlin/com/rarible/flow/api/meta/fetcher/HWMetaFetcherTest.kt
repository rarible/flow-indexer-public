package com.rarible.flow.api.meta.fetcher

import com.nftco.flow.sdk.FlowEvent
import com.nftco.flow.sdk.FlowEventPayload
import com.nftco.flow.sdk.FlowId
import com.nftco.flow.sdk.cadence.EventField
import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.UInt256NumberField
import com.nftco.flow.sdk.impl.AsyncFlowAccessApiImpl
import com.rarible.blockchain.scanner.flow.service.Spork
import com.rarible.blockchain.scanner.flow.service.SporkService
import com.rarible.core.test.data.randomByteArray
import com.rarible.core.test.data.randomString
import com.rarible.flow.api.service.HWMetaEventTypeProvider
import com.rarible.flow.core.domain.FlowActivityType
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.test.randomFlowEventResult
import com.rarible.flow.core.test.randomItemId
import com.rarible.flow.core.test.randomMintItemHistory
import com.rarible.flow.core.test.with
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.daemon.common.toHexString
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import java.util.concurrent.CompletableFuture

@Suppress("ReactiveStreamsUnusedPublisher")
class HWMetaFetcherTest {
    private val itemHistoryRepository = mockk<ItemHistoryRepository>()
    private val hwMetaEventTypeProvider = mockk<HWMetaEventTypeProvider>()
    private val spork = mockk<Spork>()
    private val accessApi = mockk<AsyncFlowAccessApiImpl>()
    private val sporkService = mockk<SporkService>()

    private val fetcher = HWMetaFetcher(
        itemHistoryRepository = itemHistoryRepository,
        hwMetaEventTypeProvider = hwMetaEventTypeProvider,
        sporkService = sporkService,
    )

    @Test
    fun `fetch - ok`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val eventType = HWMetaEventTypeProvider.Result(randomString())
        val expectedEventIndex = 1
        val expectedTransactionId = FlowId("e9225a74ce161fad735b45fa3fd80c03d28218effa0be74876cf68648c0696d5")
        val expectedMeta = FlowEventPayload(randomByteArray())

        val history = randomMintItemHistory().with(
            eventIndex = expectedEventIndex,
            transaction = expectedTransactionId.bytes.toHexString()
        )
        val eventField = mockk<EventField> {
            every { get<Field<String>>("id") } returns UInt256NumberField(itemId.tokenId.toString())
        }
        val metaEvent = mockk<FlowEvent> {
            every { eventIndex } returns expectedEventIndex
            every { transactionId } returns expectedTransactionId
            every { event } returns eventField
            every { payload } returns expectedMeta
        }
        val eventResult = randomFlowEventResult(events = listOf(metaEvent))

        every {
            itemHistoryRepository.findItemActivity(itemId.contract, itemId.tokenId, FlowActivityType.MINT)
        } returns Flux.just(history)

        every {
            hwMetaEventTypeProvider.getMetaEventType(itemId)
        } returns eventType
        every {
            sporkService.spork(history.log.blockHeight)
        } returns spork
        every {
            spork.api
        } returns accessApi
        every {
            accessApi.getEventsForHeightRange(eventType.eventType, LongRange(history.log.blockHeight, history.log.blockHeight))
        } returns CompletableFuture.completedFuture(listOf(eventResult))

        val meta = fetcher.getContent(itemId)
        assertThat(meta).isEqualTo(expectedMeta.stringValue)
    }
}