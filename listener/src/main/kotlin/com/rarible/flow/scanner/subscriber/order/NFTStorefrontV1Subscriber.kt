package com.rarible.flow.scanner.subscriber.order

import com.rarible.flow.Contracts
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.scanner.service.SupportedNftCollectionProvider
import org.springframework.stereotype.Component

@Component
class NFTStorefrontV1Subscriber(
    supportedNftCollectionProvider: SupportedNftCollectionProvider,
    orderRepository: OrderRepository
): AbstractNFTStorefrontSubscriber(supportedNftCollectionProvider, orderRepository) {

    override val name = "nft_storefront"
    override val contract = Contracts.NFT_STOREFRONT
}