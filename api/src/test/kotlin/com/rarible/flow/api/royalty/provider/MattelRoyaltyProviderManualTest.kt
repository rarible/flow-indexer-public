package com.rarible.flow.api.royalty.provider

import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.service.AsyncFlowAccessApiImpl
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.config.AppProperties
import com.rarible.flow.core.domain.Item
import io.grpc.ManagedChannelBuilder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.onflow.protobuf.access.AccessAPIGrpc
import java.math.BigDecimal
import java.net.URI

@Disabled
class MattelRoyaltyProviderManualTest {
    private val api = run {
        val node = URI.create("https://access.mainnet.nodes.onflow.org:9000")
        val channel = ManagedChannelBuilder.forAddress(node.host, node.port)
            .maxInboundMessageSize(33554432)
            .usePlaintext()
            .userAgent(Flow.DEFAULT_USER_AGENT)
            .build()
        AsyncFlowAccessApiImpl(AccessAPIGrpc.newFutureStub(channel))
    }
    private val appProperties = mockk<AppProperties> {
        every { chainId } returns FlowChainId.MAINNET
    }

    private val executor = ScriptExecutor(
        api = api,
        appProperties = appProperties
    )

    @Test
    fun `get royalties - pack V2`() = runBlocking<Unit> {
        val provider = HWGaragePackV2RoyaltyProvider(
            FlowChainId.MAINNET,
            executor
        )
        val item = mockk<Item> {
            every { owner } returns FlowAddress("0xfe43a2bd8f799f84")
            every { tokenId } returns 33694
        }
        val royalties = provider.getRoyalties(item)
        assertThat(royalties).isNotEmpty
        assertThat(royalties[0].address).isEqualTo("0xf86e2f015cd692be")
        assertThat(royalties[0].fee).isEqualTo(BigDecimal("0.05"))
    }

    @Test
    fun `get royalties - pack`() = runBlocking<Unit> {
        val provider = HWGaragePackRoyaltyProvider(
            FlowChainId.MAINNET,
            executor
        )
        val item = mockk<Item> {
            every { owner } returns FlowAddress("0xfedbb024b654874a")
            every { tokenId } returns 30780
        }
        val royalties = provider.getRoyalties(item)
        assertThat(royalties).isNotEmpty
        assertThat(royalties[0].address).isEqualTo("0xf86e2f015cd692be")
        assertThat(royalties[0].fee).isEqualTo(BigDecimal("0.05"))
    }

    @Test
    fun `get royalties - card`() = runBlocking<Unit> {
        val provider = HWGarageCardRoyaltyProvider(
            FlowChainId.MAINNET,
            executor
        )
        val item = mockk<Item> {
            every { owner } returns FlowAddress("0x13d36347c8140472")
            every { tokenId } returns 189785
        }
        val royalties = provider.getRoyalties(item)
        assertThat(royalties).isNotEmpty
        assertThat(royalties[0].address).isEqualTo("0xf86e2f015cd692be")
        assertThat(royalties[0].fee).isEqualTo(BigDecimal("0.05"))
    }

    @Test
    fun `get royalties - card V2`() = runBlocking<Unit> {
        val provider = HWGarageCardV2RoyaltyProvider(
            FlowChainId.MAINNET,
            executor
        )
        val item = mockk<Item> {
            every { owner } returns FlowAddress("0x4c79a676e507da1a")
            every { tokenId } returns 167968
        }
        val royalties = provider.getRoyalties(item)
        assertThat(royalties).isNotEmpty
        assertThat(royalties[0].address).isEqualTo("0xf86e2f015cd692be")
        assertThat(royalties[0].fee).isEqualTo(BigDecimal("0.05"))
    }

    @Test
    fun `get royalties - barbie card`() = runBlocking<Unit> {
        val provider = BarbieCardRoyaltyProvider(
            FlowChainId.MAINNET,
            executor
        )
        val item = mockk<Item> {
            every { owner } returns FlowAddress("0xba6a595ec27262e0")
            every { tokenId } returns 11355
        }
        val royalties = provider.getRoyalties(item)
        assertThat(royalties).isNotEmpty
        assertThat(royalties[0].address).isEqualTo("0xf86e2f015cd692be")
        assertThat(royalties[0].fee).isEqualTo(BigDecimal("0.05"))
    }

    @Test
    fun `get royalties - barbie pack`() = runBlocking<Unit> {
        val provider = BarbiePackRoyaltyProvider(
            FlowChainId.MAINNET,
            executor
        )
        val item = mockk<Item> {
            every { owner } returns FlowAddress("0x4246ef89b257ab4e")
            every { tokenId } returns 3101
        }
        val royalties = provider.getRoyalties(item)
        assertThat(royalties).isNotEmpty
        assertThat(royalties[0].address).isEqualTo("0xf86e2f015cd692be")
        assertThat(royalties[0].fee).isEqualTo(BigDecimal("0.05"))
    }
}
