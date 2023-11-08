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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

// For manual test only
@Disabled
class GamisodesMetaMt {

    @Nested
    inner class Testnet {

        fun testnet() = provider(
            from = 129578014L,
            nodeUrl = "access.devnet.nodes.onflow.org",
            chainId = FlowChainId.TESTNET
        )

        @Test
        fun `get meta for random item`() = runBlocking<Unit> {
            val provider = testnet()
            val item = item(
                contract = FlowAddress("09e04bdbcccde6ca"),
                collection = "A.371ebe4bc55f8925.Gamisodes",
                tokenId = 64202L,
                owner = "0xddc62c43fe61a549"
            )
            val meta = provider.getMeta(item)
            assertThat(meta).isNotNull
        }
    }

    @Nested
    inner class Prod {

        fun prod() = provider(
            from = 19050753L,
            nodeUrl = "access.mainnet.nodes.onflow.org",
            chainId = FlowChainId.MAINNET
        )

        @Test
        fun `get meta for random item`() = runBlocking<Unit> {
            val provider = prod()
            val item = item(
                contract = FlowAddress("09e04bdbcccde6ca"),
                collection = "A.09e04bdbcccde6ca.Gamisodes",
                tokenId = 5L,
                owner = "0x1d2545bc37616371"
            )
            val meta = provider.getMeta(item)
            assertThat(meta).isNotNull
        }

        @Test
        fun `fetch meta - latest item`() = runBlocking<Unit> {
            val provider = prod()
            val item = item(
                contract = FlowAddress("09e04bdbcccde6ca"),
                collection = "A.09e04bdbcccde6ca.Gamisodes",
                tokenId = 928233L,
                owner = "0x0b2ac77dbfe92266"
            )
            val meta = provider.getMeta(item)
            assertThat(meta).isNotNull
        }

        @Test
        fun `fetch meta - somewhere in the middle`() = runBlocking<Unit> {
            val provider = prod()
            val item = item(
                contract = FlowAddress("09e04bdbcccde6ca"),
                collection = "A.09e04bdbcccde6ca.Gamisodes",
                tokenId = 756378L,
                owner = "0xbd31f13c8e3b2a48"
            )
            val meta = provider.getMeta(item)
            assertThat(meta).isNotNull
        }
    }

    fun provider(
        from: Long,
        nodeUrl: String,
        chainId: FlowChainId
    ): GamisodesMetaProvider {
        val spork = Spork(from = from, nodeUrl = nodeUrl)
        val client = FlowApiFactoryImpl(
            blockchainMonitor = BlockchainMonitor(meterRegistry = SimpleMeterRegistry()),
            flowBlockchainScannerProperties = FlowBlockchainScannerProperties()
        ).getApi(spork)

        val appProperties = AppProperties(
            chainId = chainId,
            environment = "",
            flowAccessPort = 1,
            flowAccessUrl = "",
            kafkaReplicaSet = "",
            webApiUrl = ""
        )

        val ff = FeatureFlagsProperties().copy(enableRawOnChainMetaCacheRead = false, enableRawOnChainMetaCacheWrite = false)
        val scriptExecutor = ScriptExecutor(client, appProperties)
        val repository: RawOnChainMetaCacheRepository = mockk()
        return GamisodesMetaProvider(scriptExecutor, appProperties, repository, ff)
    }

    fun item(contract: FlowAddress, collection: String, tokenId: Long, owner: String) = Item(
        contract = contract.formatted,
        tokenId = tokenId,
        collection = collection,
        creator = contract,
        royalties = emptyList(),
        owner = FlowAddress(owner),
        mintedAt = Instant.now(),
        updatedAt = Instant.now()
    )
}
