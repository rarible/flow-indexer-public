package com.rarible.flow.api.meta.provider

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.configuration.FlowBlockchainScannerProperties
import com.rarible.blockchain.scanner.flow.service.FlowApiFactoryImpl
import com.rarible.blockchain.scanner.flow.service.Spork
import com.rarible.blockchain.scanner.monitoring.BlockchainMonitor
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.config.AppProperties
import com.rarible.flow.core.config.FeatureFlagsProperties
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.repository.RawOnChainMetaCacheRepository
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Instant

// For manual test only
@Disabled
class GamisodesMetaMt {

    val spork = Spork(from = 19050753L, nodeUrl = "access.mainnet.nodes.onflow.org")
    val client = FlowApiFactoryImpl(
        blockchainMonitor = BlockchainMonitor(meterRegistry = SimpleMeterRegistry()),
        flowBlockchainScannerProperties = FlowBlockchainScannerProperties()
    ).getApi(spork)

    val appProperties = AppProperties(
        chainId = FlowChainId.MAINNET,
        environment = "",
        flowAccessPort = 1,
        flowAccessUrl = "",
        kafkaReplicaSet = "",
        webApiUrl = ""
    )

    val ff = FeatureFlagsProperties().copy(enableRawOnChainMetaCacheRead = false, enableRawOnChainMetaCacheWrite = false)
    val scriptExecutor = ScriptExecutor(client, appProperties)
    val repository: RawOnChainMetaCacheRepository = mockk()
    val provider = GamisodesMetaProvider(scriptExecutor, appProperties, repository, ff)
    val collection = FlowAddress("09e04bdbcccde6ca")

    @Test
    fun `fetch meta - old item`() = runBlocking<Unit> {
        val item = Item(
            contract = collection.formatted,
            tokenId = 5L,
            collection = "A.09e04bdbcccde6ca.Gamisodes",
            creator = collection,
            royalties = emptyList(),
            owner = FlowAddress("0x1d2545bc37616371"),
            mintedAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val meta = provider.getMeta(item)
        assertThat(meta).isNotNull
    }

    @Test
    fun `fetch meta - latest item`() = runBlocking<Unit> {
        val item = Item(
            contract = collection.formatted,
            tokenId = 928233L,
            collection = "A.09e04bdbcccde6ca.Gamisodes",
            creator = collection,
            royalties = emptyList(),
            owner = FlowAddress("0x0b2ac77dbfe92266"),
            mintedAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val meta = provider.getMeta(item)
        assertThat(meta).isNotNull
    }

    // TODO works with 'GamisodesCollection'
    @Test
    fun `fetch meta - somewhere in the middle`() = runBlocking<Unit> {
        val item = Item(
            contract = collection.formatted,
            tokenId = 756378L,
            owner = FlowAddress("0xbd31f13c8e3b2a48"),
            collection = "A.09e04bdbcccde6ca.Gamisodes",
            creator = collection,
            royalties = emptyList(),
            mintedAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val meta = provider.getMeta(item)
        assertThat(meta).isNotNull
    }
}
