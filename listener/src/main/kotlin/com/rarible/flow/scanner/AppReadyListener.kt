package com.rarible.flow.scanner

import com.rarible.blockchain.scanner.flow.configuration.FlowBlockchainScannerProperties
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.ItemCollection
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.repository.ItemCollectionRepository
import com.rarible.flow.core.service.SporkConfigurationService
import com.rarible.protocol.dto.FlowEventTimeMarksDto
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

@Component
class AppReadyListener(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val sporkConfigurationService: SporkConfigurationService,
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val itemCollectionRepository: ItemCollectionRepository,
    private val scannerProperties: FlowBlockchainScannerProperties,
    private val eventPublisher: ProtocolEventPublisher
) : ApplicationListener<ApplicationReadyEvent> {

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        runBlocking {
            val supportedCollections = Contracts.values()
                .filter { it.nft && it.enabled }
                .mapNotNull { it.toItemCollection(scannerProperties.chainId) }

            supportedCollections
                .chunked(COLLECTION_HANDLE_BATCH_SIZE)
                .map { chunk ->
                    async { chunk.map { addCollection(it) } }
                }
                .awaitAll()

            sporkConfigurationService.config()
        }
    }

    private suspend fun addCollection(itemCollection: ItemCollection) {
        val existed = itemCollectionRepository.findById(itemCollection.id).awaitFirstOrNull()
        if (existed != itemCollection) {
            eventPublisher.onCollection(itemCollection, FlowEventTimeMarksDto("add-collection"))
            itemCollectionRepository.save(itemCollection).awaitFirst()
        }
    }

    private companion object {
        const val COLLECTION_HANDLE_BATCH_SIZE = 100
    }
}