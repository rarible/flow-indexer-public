package com.rarible.flow.scanner

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.configuration.FlowBlockchainScannerProperties
import com.rarible.blockchain.scanner.flow.service.SporkService
import com.rarible.flow.core.domain.ItemCollection
import com.rarible.flow.core.repository.ItemCollectionRepository
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
) : ApplicationListener<ApplicationReadyEvent> {

    private val supportedCollections = mapOf(
        FlowChainId.TESTNET to listOf(
            ItemCollection(id = "A.ebf4ae01d1284af8.RaribleNFT", name = "Rarible", owner = FlowAddress("0xebf4ae01d1284af8"), symbol = "RARIBLE", features = setOf("SECONDARY_SALE_FEES", "BURN")),
            ItemCollection(id = "A.01658d9b94068f3c.MotoGPCard", name = "MotoGP™ Ignition", owner = FlowAddress("0x01658d9b94068f3c"), symbol = "MotoGP™", features = setOf("BURN")),
            ItemCollection(id = "A.01658d9b94068f3c.TopShot", name = "NBA Top Shot", owner = FlowAddress("0x01658d9b94068f3c"), symbol = "NBA TS", features = setOf("BURN")),
            ItemCollection(id = "A.01658d9b94068f3c.Evolution", name = "Evolution", owner = FlowAddress("0x01658d9b94068f3c"), symbol = "EVOLUTION", features = setOf("BURN")),
            ItemCollection(id = "A.ebf4ae01d1284af8.MugenNFT", name = "Mugen", owner = FlowAddress("0xebf4ae01d1284af8"), symbol = "MUGEN", features = setOf()),
        ),
        FlowChainId.MAINNET to listOf(
            ItemCollection(id = "A.01ab36aaf654a13e.RaribleNFT", name = "Rarible", owner = FlowAddress("0x01ab36aaf654a13e"), symbol = "RARIBLE", features = setOf("SECONDARY_SALE_FEES", "BURN")),
            ItemCollection(id = "A.a49cc0ee46c54bfb.MotoGPCard", name = "MotoGP™ Ignition", owner = FlowAddress("0xa49cc0ee46c54bfb"), symbol = "MotoGP™", features = setOf("BURN")),
            ItemCollection(id = "A.0b2a3299cc857e29.TopShot", name = "NBA Top Shot", owner = FlowAddress("0x0b2a3299cc857e29"), symbol = "NBA TS", features = setOf("BURN")),
            ItemCollection(id = "A.f4264ac8f3256818.Evolution", name = "Evolution", owner = FlowAddress("0xf4264ac8f3256818"), symbol = "EVOLUTION", features = setOf("BURN")),
            ItemCollection(id = "A.2cd46d41da4ce262.MugenNFT", name = "Mugen", owner = FlowAddress("0x2cd46d41da4ce262"), symbol = "MUGEN", features = setOf()),
        )
    )

    /**
     * Save default item collection's
     */
    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        if (scannerProperties.chainId != FlowChainId.EMULATOR) {
            runBlocking {
                itemCollectionRepository.deleteAll().awaitFirstOrNull()
                itemCollectionRepository.saveAll(supportedCollections[scannerProperties.chainId]!!).then()
                    .awaitFirstOrNull()

                if (scannerProperties.chainId == FlowChainId.MAINNET) {
                    val head = listOf(
                        SporkService.Spork(
                            from = 21291692L,
                            nodeUrl = "access.mainnet.nodes.onflow.org"
                        ),
                        SporkService.Spork(
                            from = 19050753L,
                            to = 21291691L,
                            nodeUrl = "access-001.mainnet14.nodes.onflow.org"
                        ),
                    )
                    val tail = sporkService.allSporks[FlowChainId.MAINNET]!!.drop(1)
                    sporkService.allSporks.replace(FlowChainId.MAINNET, head + tail)
                }
            }
        }
    }
}
