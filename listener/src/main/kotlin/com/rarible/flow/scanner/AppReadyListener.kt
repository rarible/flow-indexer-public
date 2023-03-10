package com.rarible.flow.scanner

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.configuration.FlowBlockchainScannerProperties
import com.rarible.blockchain.scanner.flow.service.Spork
import com.rarible.blockchain.scanner.flow.service.SporkService
import com.rarible.flow.Contracts
import com.rarible.flow.core.repository.ItemCollectionRepository
import com.rarible.flow.scanner.config.FlowListenerProperties
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

@Component
class AppReadyListener(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val itemCollectionRepository: ItemCollectionRepository,
    private val scannerProperties: FlowBlockchainScannerProperties,
    private val sporkService: SporkService,
    private val properties: FlowListenerProperties,
) : ApplicationListener<ApplicationReadyEvent> {

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        runBlocking {
            val supportedCollections = Contracts.values()
                .filter { it.nft }
                .mapNotNull { it.toItemCollection(scannerProperties.chainId) }

            itemCollectionRepository.saveAll(supportedCollections)
                .then()
                .awaitFirstOrNull()

            if (scannerProperties.chainId == FlowChainId.EMULATOR) {
                sporkService.replace(FlowChainId.EMULATOR, listOf(
                    Spork(
                        from = 0,
                        to = Long.MAX_VALUE,
                        nodeUrl = properties.flowAccessUrl,
                        port = properties.flowAccessPort,
                    )
                ))
            }
            if (scannerProperties.chainId == FlowChainId.TESTNET) {
                sporkService.replace(FlowChainId.TESTNET, listOf(
                    Spork(
                        from = 93156994,
                        nodeUrl = "access.devnet.nodes.onflow.org"
                    )
                ))
            }
            if (scannerProperties.chainId == FlowChainId.MAINNET) {
                val head = listOf(
                    Spork(
                        from = 47169687,
                        nodeUrl = "access.mainnet.nodes.onflow.org"
                    ),
                    Spork(
                        from = 44950207,
                        to = 47169686,
                        nodeUrl = "access-001.mainnet21.nodes.onflow.org",
                    ),
                    Spork(
                        from = 40171634,
                        to = 44950206,
                        nodeUrl = "access-001.mainnet20.nodes.onflow.org",
                    ),
                    Spork(
                        from = 35858811L,
                        to = 40171633L,
                        nodeUrl = "access-001.mainnet19.nodes.onflow.org",
                    ),
                    Spork(
                        from = 35858811L,
                        to = 40171633L,
                        nodeUrl = "access-001.mainnet19.nodes.onflow.org",
                    ),
                    Spork(
                        from = 31735955L,
                        to = 35858810L,
                        nodeUrl = "access-001.mainnet18.nodes.onflow.org",
                    ),
                    Spork(
                        from = 27341470L,
                        to = 31735954L,
                        nodeUrl = "access-001.mainnet17.nodes.onflow.org",
                    ),
                    Spork(
                        from = 23830813L,
                        to = 27341469L,
                        nodeUrl = "access-001.mainnet16.nodes.onflow.org",
                    ),
                    Spork(
                        from = 21291692L,
                        to = 23830812L,
                        nodeUrl = "access-001.mainnet15.nodes.onflow.org"
                    ),
                    Spork(
                        from = 19050753L,
                        to = 21291691L,
                        nodeUrl = "access-001.mainnet14.nodes.onflow.org"
                    ),
                )
                val tail = sporkService.sporks(FlowChainId.MAINNET).drop(1)
                sporkService.replace(FlowChainId.MAINNET, head + tail)
            }
        }
    }
}
