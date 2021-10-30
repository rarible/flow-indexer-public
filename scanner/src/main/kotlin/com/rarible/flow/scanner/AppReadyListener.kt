package com.rarible.flow.scanner

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.configuration.FlowBlockchainScannerProperties
import com.rarible.flow.core.domain.ItemCollection
import com.rarible.flow.core.repository.ItemCollectionRepository
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

@Component
class AppReadyListener(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val itemCollectionRepository: ItemCollectionRepository,
    private val scannerProperties: FlowBlockchainScannerProperties
): ApplicationListener<ApplicationReadyEvent> {

    private val  supportedCollections = mapOf(
        FlowChainId.TESTNET to listOf(
            ItemCollection(id = "A.01658d9b94068f3c.RaribleNFT", name = "Rarible", owner = FlowAddress("0x01658d9b94068f3c"), symbol = "RARIBLE"),
            ItemCollection(id = "A.01658d9b94068f3c.MotoGPCard", name = "MotoGP™ Ignition", owner = FlowAddress("0x01658d9b94068f3c"), symbol = "MotoGP™"),
            ItemCollection(id = "A.01658d9b94068f3c.TopShot", name = "NBA Top Shot", owner = FlowAddress("0x01658d9b94068f3c"), symbol = "NBA TS"),
            ItemCollection(id = "A.01658d9b94068f3c.Evolution", name = "Evolution", owner = FlowAddress("0x01658d9b94068f3c"), symbol = "EVOLUTION"),
        ),
        FlowChainId.MAINNET to listOf(
            ItemCollection(id = "A.1ab36aaf654a13e.RaribleNFT", name = "Rarible", owner = FlowAddress("0x01ab36aaf654a13e"), symbol = "RARIBLE"),
            ItemCollection(id = "A.a49cc0ee46c54bfb.MotoGPCard", name = "MotoGP™ Ignition", owner = FlowAddress("0xa49cc0ee46c54bfb"), symbol = "MotoGP™"),
            ItemCollection(id = "A.0b2a3299cc857e29.TopShot", name = "NBA Top Shot", owner = FlowAddress("0x0b2a3299cc857e29"), symbol = "NBA TS"),
            ItemCollection(id = "A.f4264ac8f3256818.Evolution", name = "Evolution", owner = FlowAddress("0xf4264ac8f3256818"), symbol = "EVOLUTION"),
        )
    )

    /**
     * Save default item collection's
     */
    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        if (scannerProperties.chainId != FlowChainId.EMULATOR) {
            itemCollectionRepository.deleteAll().block()
            itemCollectionRepository.saveAll(supportedCollections[scannerProperties.chainId]!!).then().block()
        }
    }
}
