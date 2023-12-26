package com.rarible.flow.scanner.subscriber.order

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.Contracts
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.scanner.service.SupportedNftCollectionProvider
import org.springframework.stereotype.Component

@Component
class NFTStorefrontV2Subscriber(
    supportedNftCollectionProvider: SupportedNftCollectionProvider,
    orderRepository: OrderRepository,
    chainId: FlowChainId,
) : AbstractNFTStorefrontSubscriber(supportedNftCollectionProvider, orderRepository, chainId) {

    override val name = "nft_storefront_v2"
    override val contract = Contracts.NFT_STOREFRONT_V2
}
