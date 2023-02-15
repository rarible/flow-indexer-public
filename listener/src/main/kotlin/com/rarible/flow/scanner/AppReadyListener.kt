package com.rarible.flow.scanner

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.configuration.FlowBlockchainScannerProperties
import com.rarible.blockchain.scanner.flow.service.SporkService
import com.rarible.core.daemon.sequential.ConsumerWorkerHolder
import com.rarible.flow.Contracts
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
            ItemCollection(id = "A.ebf4ae01d1284af8.MugenNFT", name = "Mugen", owner = FlowAddress("0xebf4ae01d1284af8"), symbol = "MUGEN", features = emptySet()),
            ItemCollection(id = "A.ebf4ae01d1284af8.CNN_NFT", name = "CNN", owner = FlowAddress("0xebf4ae01d1284af8"), symbol = "CNN", features = setOf("BURN")),
            ItemCollection(id = "A.ebf4ae01d1284af8.Art", name = "VersusArt", owner = FlowAddress("0xebf4ae01d1284af8"), symbol = "VERSUS", features = setOf()),
            ItemCollection(id = "A.439c2b49c0b2f62b.DisruptArt", name = "DisruptArt", owner = FlowAddress("0x439c2b49c0b2f62b"), symbol = "DA", features = emptySet()),
            ItemCollection(
                id = Contracts.ONE_FOOTBALL.fqn(FlowChainId.TESTNET),
                name = "OneFootball",
                owner = Contracts.ONE_FOOTBALL.deployments[FlowChainId.TESTNET]!!,
                symbol = "ONEFOOTBALL",
                features = setOf("BURN")
            ),
            ItemCollection(
                id = Contracts.MATRIX_WORLD_VOUCHER.fqn(FlowChainId.TESTNET),
                name = "Matrix World Voucher",
                owner = Contracts.MATRIX_WORLD_VOUCHER.deployments[FlowChainId.TESTNET]!!,
                symbol = "MXWRLDV",
                features = emptySet()
            ),
            ItemCollection(
                id = Contracts.MATRIX_WORLD_FLOW_FEST.fqn(FlowChainId.TESTNET),
                name = "Matrix World Flow Fest",
                owner = Contracts.MATRIX_WORLD_FLOW_FEST.deployments[FlowChainId.TESTNET]!!,
                symbol = "MXWRLDFFEST",
                features = emptySet()
            ),
            ItemCollection(
                id = Contracts.JAMBB_MOMENTS.fqn(FlowChainId.TESTNET),
                name = "Jambb",
                owner = Contracts.JAMBB_MOMENTS.deployments[FlowChainId.TESTNET]!!,
                symbol = "JAMBB",
                features = setOf("BURN")
            ),
            ItemCollection(
                id = Contracts.FANFARE.fqn(FlowChainId.TESTNET),
                name = "Fanfare",
                owner = Contracts.FANFARE.deployments[FlowChainId.TESTNET]!!,
                symbol = "FANFARE",
                features = emptySet()
            ),
            ItemCollection(
                id = Contracts.RARIBLE_NFTV2.fqn(FlowChainId.TESTNET),
                name = "RaribleV2",
                owner = Contracts.RARIBLE_NFTV2.deployments[FlowChainId.TESTNET]!!,
                symbol = "RARIBLE_V2",
                features = setOf("SECONDARY_SALE_FEES", "BURN")
            ),
            ItemCollection(
                id = Contracts.CHAINMONSTERS.fqn(FlowChainId.TESTNET),
                name = "Chainmonsters",
                owner = Contracts.CHAINMONSTERS.deployments[FlowChainId.TESTNET]!!,
                symbol = "CHAINMONSTERS",
                features = emptySet()
            ),
            ItemCollection(
                id = Contracts.BARTER_YARD_PACK.fqn(FlowChainId.TESTNET),
                name = "Barter Yard Club - Mint Pass",
                owner = Contracts.BARTER_YARD_PACK.deployments[FlowChainId.TESTNET]!!,
                symbol = "BYC-MP",
                features = setOf("BURN")
            ),
            ItemCollection(
                id = Contracts.KICKS.fqn(FlowChainId.TESTNET),
                name = "Kicks",
                owner = Contracts.KICKS.deployments[FlowChainId.TESTNET]!!,
                symbol = "KICKS",
                features = setOf("BURN")
            ),
            ItemCollection(
                id = Contracts.SOME_PLACE_COLLECTIBLE.fqn(FlowChainId.TESTNET),
                name = "The Poison",
                owner = Contracts.SOME_PLACE_COLLECTIBLE.deployments[FlowChainId.TESTNET]!!,
                symbol = "POISON",
                features = setOf("BURN")
            ),
            ItemCollection(
                id = Contracts.GENIACE.fqn(FlowChainId.TESTNET),
                name = "GENIACE",
                owner = Contracts.GENIACE.deployments[FlowChainId.TESTNET]!!,
                symbol = "GEN",
            ),
            ItemCollection(
                id = Contracts.CRYPTOPIGGO.fqn(FlowChainId.TESTNET),
                name = "CryptoPiggos",
                owner = Contracts.CRYPTOPIGGO.deployments[FlowChainId.TESTNET]!!,
                symbol = "CPIG",
                features = emptySet()
            ),
        ),

        FlowChainId.MAINNET to listOf(
            ItemCollection(id = "A.01ab36aaf654a13e.RaribleNFT", name = "Rarible", owner = FlowAddress("0x01ab36aaf654a13e"), symbol = "RARIBLE", features = setOf("SECONDARY_SALE_FEES", "BURN")),
            ItemCollection(id = "A.a49cc0ee46c54bfb.MotoGPCard", name = "MotoGP™ Ignition", owner = FlowAddress("0xa49cc0ee46c54bfb"), symbol = "MotoGP™", features = setOf("BURN")),
            ItemCollection(id = "A.0b2a3299cc857e29.TopShot", name = "NBA Top Shot", owner = FlowAddress("0x0b2a3299cc857e29"), symbol = "NBA TS", features = setOf("BURN")),
            ItemCollection(id = "A.f4264ac8f3256818.Evolution", name = "Evolution", owner = FlowAddress("0xf4264ac8f3256818"), symbol = "EVOLUTION", features = setOf("BURN")),
            ItemCollection(id = "A.2cd46d41da4ce262.MugenNFT", name = "Mugen", owner = FlowAddress("0x2cd46d41da4ce262"), symbol = "MUGEN", features = emptySet()),
            ItemCollection(id = "A.329feb3ab062d289.CNN_NFT", name = "CNN", owner = FlowAddress("0x329feb3ab062d289"), symbol = "CNN", features = setOf("BURN")),
            ItemCollection(id = "A.d796ff17107bbff6.Art", name = "VersusArt", owner = FlowAddress("0xd796ff17107bbff6"), symbol = "VERSUS", features = setOf()),
            ItemCollection(id = "A.cd946ef9b13804c6.DisruptArt", name = "DisruptArt", owner = FlowAddress("0xcd946ef9b13804c6"), symbol = "DISRUPT ART", features = emptySet()),
            ItemCollection(
                id = Contracts.ONE_FOOTBALL.fqn(FlowChainId.MAINNET),
                name = "OneFootball",
                owner = Contracts.ONE_FOOTBALL.deployments[FlowChainId.MAINNET]!!,
                symbol = "ONEFOOTBALL",
                features = setOf("BURN")
            ),
            ItemCollection(
                id = Contracts.MATRIX_WORLD_VOUCHER.fqn(FlowChainId.MAINNET),
                name = "Matrix World Voucher",
                owner = Contracts.MATRIX_WORLD_VOUCHER.deployments[FlowChainId.MAINNET]!!,
                symbol = "MXWRLDV",
                features = emptySet()
            ),
            ItemCollection(
                id = Contracts.MATRIX_WORLD_FLOW_FEST.fqn(FlowChainId.MAINNET),
                name = "Matrix World Flow Fest",
                owner = Contracts.MATRIX_WORLD_FLOW_FEST.deployments[FlowChainId.MAINNET]!!,
                symbol = "MXWRLDFFEST",
                features = emptySet()
            ),
            ItemCollection(
                id = Contracts.STARLY_CARD.fqn(FlowChainId.MAINNET),
                name = "Starly",
                owner = Contracts.STARLY_CARD.deployments[FlowChainId.MAINNET]!!,
                symbol = "STARLY",
                features = setOf("BURN")
            ),
            ItemCollection(
                id = Contracts.JAMBB_MOMENTS.fqn(FlowChainId.MAINNET),
                name = "Jambb",
                owner = Contracts.JAMBB_MOMENTS.deployments[FlowChainId.MAINNET]!!,
                symbol = "JAMBB",
                features = setOf("BURN")
            ),
            ItemCollection(
                id = Contracts.FANFARE.fqn(FlowChainId.MAINNET),
                name = "Fanfare",
                owner = Contracts.FANFARE.deployments[FlowChainId.MAINNET]!!,
                symbol = "FANFARE",
                features = emptySet()
            ),
            ItemCollection(
                id = Contracts.RARIBLE_NFTV2.fqn(FlowChainId.MAINNET),
                name = "RaribleV2",
                owner = Contracts.RARIBLE_NFTV2.deployments[FlowChainId.MAINNET]!!,
                symbol = "RARIBLE_V2",
                features = setOf("SECONDARY_SALE_FEES", "BURN")
            ),
            ItemCollection(
                id = Contracts.CHAINMONSTERS.fqn(FlowChainId.MAINNET),
                name = "Chainmonsters",
                owner = Contracts.CHAINMONSTERS.deployments[FlowChainId.MAINNET]!!,
                symbol = "CHAINMONSTERS",
                features = emptySet()
            ),
            ItemCollection(
                id = Contracts.BARTER_YARD_PACK.fqn(FlowChainId.MAINNET),
                name = "Barter Yard Club - Mint Pass",
                owner = Contracts.BARTER_YARD_PACK.deployments[FlowChainId.MAINNET]!!,
                symbol = "BYC-MP",
                features = setOf("BURN")
            ),
            ItemCollection(
                id = Contracts.KICKS.fqn(FlowChainId.MAINNET),
                name = "Kicks",
                owner = Contracts.KICKS.deployments[FlowChainId.MAINNET]!!,
                symbol = "KICKS",
                features = setOf("BURN")
            ),
            ItemCollection(
                id = Contracts.SOME_PLACE_COLLECTIBLE.fqn(FlowChainId.MAINNET),
                name = "The Potion",
                owner = Contracts.SOME_PLACE_COLLECTIBLE.deployments[FlowChainId.MAINNET]!!,
                symbol = "POTION",
                features = setOf("BURN")
            ),
            ItemCollection(
                id = Contracts.GENIACE.fqn(FlowChainId.MAINNET),
                name = "Geniace",
                owner = Contracts.GENIACE.deployments[FlowChainId.MAINNET]!!,
                symbol = "GEN",
            ),
            ItemCollection(
                id = Contracts.CRYPTOPIGGO.fqn(FlowChainId.MAINNET),
                name = "CryptoPiggos",
                owner = Contracts.CRYPTOPIGGO.deployments[FlowChainId.MAINNET]!!,
                symbol = "CPIG",
                features = emptySet()
            ),
        ),
    )

    /**
     * Save default item collection's
     */
    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        if (scannerProperties.chainId != FlowChainId.EMULATOR) {
            runBlocking {
                itemCollectionRepository.saveAll(supportedCollections[scannerProperties.chainId]!!).then()
                    .awaitFirstOrNull()

                if (scannerProperties.chainId == FlowChainId.TESTNET) {
                    sporkService.allSporks.replace(FlowChainId.TESTNET, listOf(
                        SporkService.Spork(from = 83007730, nodeUrl = "access.devnet.nodes.onflow.org"),
                    ))
                }

                if (scannerProperties.chainId == FlowChainId.MAINNET) {
                    val head = listOf(
                        SporkService.Spork(
                            from = 40171634L,
                            nodeUrl = "access.mainnet.nodes.onflow.org"
                        ),
                        SporkService.Spork(
                            from = 35858811L,
                            to = 40171633L,
                            nodeUrl = "access-001.mainnet19.nodes.onflow.org",
                        ),
                        SporkService.Spork(
                            from = 31735955L,
                            to = 35858810L,
                            nodeUrl = "access-001.mainnet18.nodes.onflow.org",
                        ),
                        SporkService.Spork(
                            from = 27341470L,
                            to = 31735954L,
                            nodeUrl = "access-001.mainnet17.nodes.onflow.org",
                        ),
                        SporkService.Spork(
                            from = 23830813L,
                            to = 27341469L,
                            nodeUrl = "access-001.mainnet16.nodes.onflow.org",
                        ),
                        SporkService.Spork(
                            from = 21291692L,
                            to = 23830812L,
                            nodeUrl = "access-001.mainnet15.nodes.onflow.org"
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
