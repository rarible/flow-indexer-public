package com.rarible.flow.scanner.subscriber.order

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.Contracts
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.scanner.service.SupportedNftCollectionProvider
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NFTStorefrontV2SubscriberTest {
    private val orderRepository = mockk<OrderRepository>()
    private val supportedNftCollectionProvider = mockk<SupportedNftCollectionProvider> {
        every { get() } returns emptySet()
    }
    private val storefrontV2 = NFTStorefrontV2Subscriber(
        supportedNftCollectionProvider = supportedNftCollectionProvider,
        orderRepository = orderRepository
    )

    @Test
    fun `get descriptor map`() {
        val descriptors = storefrontV2.descriptors
        val testnet = descriptors[FlowChainId.TESTNET]
        assertThat(testnet?.address).isEqualTo(Contracts.NFT_STOREFRONT_V2.fqn(FlowChainId.TESTNET))
    }
}
