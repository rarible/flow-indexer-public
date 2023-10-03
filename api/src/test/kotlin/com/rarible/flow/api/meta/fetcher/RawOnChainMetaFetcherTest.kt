package com.rarible.flow.api.meta.fetcher

import com.nftco.flow.sdk.FlowEvent
import com.nftco.flow.sdk.FlowEventPayload
import com.nftco.flow.sdk.FlowId
import com.nftco.flow.sdk.cadence.EventField
import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.UInt256NumberField
import com.rarible.blockchain.scanner.flow.service.AsyncFlowAccessApiImpl
import com.rarible.blockchain.scanner.flow.service.FlowApiFactory
import com.rarible.blockchain.scanner.flow.service.Spork
import com.rarible.blockchain.scanner.flow.service.SporkService
import com.rarible.core.test.data.randomByteArray
import com.rarible.core.test.data.randomString
import com.rarible.flow.api.service.meta.MetaEventType
import com.rarible.flow.core.config.FeatureFlagsProperties
import com.rarible.flow.core.domain.FlowActivityType
import com.rarible.flow.core.domain.RawOnChainMeta
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.RawOnChainMetaCacheRepository
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
import reactor.core.publisher.Mono
import java.util.concurrent.CompletableFuture

@Suppress("ReactiveStreamsUnusedPublisher")
class RawOnChainMetaFetcherTest {

    private val itemHistoryRepository = mockk<ItemHistoryRepository>()
    private val rawOnChainMetaCacheRepository = mockk<RawOnChainMetaCacheRepository>()
    private val spork = mockk<Spork>()
    private val flowApiFactory = mockk<FlowApiFactory>()
    private val accessApi = mockk<AsyncFlowAccessApiImpl>()
    private val sporkService = mockk<SporkService>()
    private val ff: FeatureFlagsProperties = FeatureFlagsProperties(
        enableRawOnChainMetaCacheRead = true,
        enableRawOnChainMetaCacheWrite = true
    )

    private val fetcher = RawOnChainMetaFetcher(
        itemHistoryRepository = itemHistoryRepository,
        sporkService = sporkService,
        rawOnChainMetaCacheRepository = rawOnChainMetaCacheRepository,
        flowApiFactory = flowApiFactory,
        ff = ff
    )

    @Test
    fun `fetch - ok, not cached`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val eventType = MetaEventType(randomString())
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

        every { rawOnChainMetaCacheRepository.findById(itemId.toString()) } returns Mono.empty()

        every { sporkService.spork(history.log.blockHeight) } returns spork
        every { flowApiFactory.getApi(spork) } returns accessApi

        every {
            accessApi.getEventsForHeightRange(
                eventType.eventType,
                LongRange(history.log.blockHeight, history.log.blockHeight)
            )
        } returns CompletableFuture.completedFuture(listOf(eventResult))

        val rawMeta = RawOnChainMeta(itemId.toString(), expectedMeta.stringValue)
        every { rawOnChainMetaCacheRepository.save(rawMeta) } returns Mono.just(rawMeta)

        val meta = fetcher.getContent(itemId, eventType)
        assertThat(meta).isEqualTo(expectedMeta.stringValue)
    }

    @Test
    fun `fetch - ok, cached`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val cachedRawMeta = RawOnChainMeta(itemId.toString(), randomString())

        every { rawOnChainMetaCacheRepository.findById(itemId.toString()) } returns Mono.just(cachedRawMeta)

        val meta = fetcher.getContent(itemId, MetaEventType(randomString()))
        assertThat(meta).isEqualTo(cachedRawMeta.data)
    }
}
