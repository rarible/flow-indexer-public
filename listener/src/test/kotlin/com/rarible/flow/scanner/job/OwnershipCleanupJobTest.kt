package com.rarible.flow.scanner.job

import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.core.test.randomOwnership
import com.rarible.flow.scanner.config.CleanUpProperties
import com.rarible.flow.scanner.config.FlowListenerProperties
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

@Suppress("ReactiveStreamsUnusedPublisher")
class OwnershipCleanupJobTest {
    private val ownershipRepository = mockk<OwnershipRepository>()
    private val protocolEventPublisher = mockk<ProtocolEventPublisher>()
    private val properties = mockk<FlowListenerProperties>()
    private val job = OwnershipCleanupJob(ownershipRepository, protocolEventPublisher, properties)

    @Test
    fun clean() = runBlocking<Unit> {
        val ownership1 = randomOwnership()
        val cleanup = CleanUpProperties()

        every { properties.cleanup } returns cleanup
        every { ownershipRepository.find(null, cleanup.batchSize) } returns flow {
            listOf(ownership1).forEach { emit(it) }
        }
        every { ownershipRepository.find(ownership1.id, cleanup.batchSize) } returns flow {
        }
        coEvery { ownershipRepository.delete(ownership1) } returns Mono.empty()
        coEvery { protocolEventPublisher.onDelete(ownership1, any()) } returns Unit

        val handled = job.execute(null).toList()

        Assertions.assertThat(handled).hasSize(1)

        coVerifyOrder {
            ownershipRepository.find(null, cleanup.batchSize)

            protocolEventPublisher.onDelete(ownership1, any())
            ownershipRepository.delete(ownership1)
        }
    }
}
