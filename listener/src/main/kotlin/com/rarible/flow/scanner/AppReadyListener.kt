package com.rarible.flow.scanner

import com.rarible.blockchain.scanner.flow.configuration.FlowBlockchainScannerProperties
import com.rarible.flow.Contracts
import com.rarible.flow.core.repository.ItemCollectionRepository
import com.rarible.flow.core.service.SporkConfigurationService
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
) : ApplicationListener<ApplicationReadyEvent> {

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        runBlocking {
            val supportedCollections = Contracts.values()
                .filter { it.nft && it.enabled }
                .mapNotNull { it.toItemCollection(scannerProperties.chainId) }

            itemCollectionRepository.saveAll(supportedCollections)
                .then()
                .awaitFirstOrNull()

            sporkConfigurationService.config()
        }
    }
}