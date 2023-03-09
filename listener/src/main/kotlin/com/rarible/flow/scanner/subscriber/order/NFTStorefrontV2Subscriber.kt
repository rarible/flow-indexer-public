package com.rarible.flow.scanner.subscriber.order

import com.rarible.flow.Contracts
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.scanner.service.SupportedNftCollectionProvider
import com.rarible.flow.scanner.subscriber.EnableStorefrontV2
import org.springframework.stereotype.Component

@Component
@EnableStorefrontV2
class NFTStorefrontV2Subscriber(
    supportedNftCollectionProvider: SupportedNftCollectionProvider,
    orderRepository: OrderRepository
): AbstractNFTStorefrontSubscriber(supportedNftCollectionProvider, orderRepository) {

    override val name = "nft_storefront_v2"
    override val contract = Contracts.NFT_STOREFRONT_V2
}
