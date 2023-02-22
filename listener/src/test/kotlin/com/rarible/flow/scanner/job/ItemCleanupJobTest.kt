package com.rarible.flow.scanner.job

import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.scanner.config.CleanUpProperties
import com.rarible.flow.scanner.config.FlowListenerProperties
import com.rarible.flow.scanner.randomItem
import com.rarible.flow.scanner.randomItemId
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

@Suppress("ReactiveStreamsUnusedPublisher")
class ItemCleanupJobTest {
    private val itemRepository = mockk<ItemRepository>()
    private val protocolEventPublisher = mockk<ProtocolEventPublisher>()
    private val properties = mockk<FlowListenerProperties>()
    private val job = ItemCleanupJob(itemRepository, protocolEventPublisher, properties)

    @Test
    fun clean() = runBlocking<Unit> {
        val cleanup = CleanUpProperties()
        val from = randomItemId()
        val item1 = randomItem()
        val item2 = randomItem()

        every { properties.cleanup } returns cleanup
        every { itemRepository.find(from, cleanup.batchSize) } returns flow { emit(item1) }
        every { itemRepository.find(item1.id, cleanup.batchSize) } returns flow { emit(item2) }
        every { itemRepository.find(item2.id, cleanup.batchSize) } returns flow { }

        coEvery { itemRepository.delete(item1) } returns Mono.empty()
        coEvery { itemRepository.delete(item2) } returns Mono.empty()
        coEvery { protocolEventPublisher.onItemDelete(item1.id, any()) } returns Unit
        coEvery { protocolEventPublisher.onItemDelete(item2.id, any()) } returns Unit

        val handled = job.execute(from).toList()

        assertThat(handled).hasSize(2)
        assertThat(handled[0]).isEqualTo(item1.id)
        assertThat(handled[1]).isEqualTo(item2.id)

        coVerifyOrder {
            itemRepository.find(from, cleanup.batchSize)

            protocolEventPublisher.onItemDelete(item1.id, any())
            itemRepository.delete(item1)
            itemRepository.find(item1.id, cleanup.batchSize)

            protocolEventPublisher.onItemDelete(item2.id, any())
            itemRepository.delete(item2)
            itemRepository.find(item2.id, cleanup.batchSize)
        }
    }

    @Test
    fun `clean - skip preserved collections`() = runBlocking<Unit> {
        val item1 = randomItem()
        val item2 = randomItem()
        val cleanup = CleanUpProperties().copy(preservedCollections = listOf(item2.contract))

        every { properties.cleanup } returns cleanup
        every { itemRepository.find(null, cleanup.batchSize) } returns flow {
            listOf(item1, item2).forEach { emit(it) }
        }
        every { itemRepository.find(item2.id, cleanup.batchSize) } returns flow {

        }
        coEvery { itemRepository.delete(item1) } returns Mono.empty()
        coEvery { protocolEventPublisher.onItemDelete(item1.id, any()) } returns Unit

        job.execute(null).toList()

        verify {
            itemRepository.delete(item1)
        }
        verify(exactly = 0) {
            itemRepository.delete(item2)
        }
    }
}