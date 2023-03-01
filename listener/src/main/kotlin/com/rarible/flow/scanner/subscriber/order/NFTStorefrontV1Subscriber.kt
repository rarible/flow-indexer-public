package com.rarible.flow.scanner.subscriber.order

import com.rarible.flow.Contracts
import com.rarible.flow.core.repository.ItemCollectionRepository
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.scanner.TxManager
import org.springframework.stereotype.Component

@Component
class NFTStorefrontV1Subscriber(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    collectionRepository: ItemCollectionRepository,
    txManager: TxManager,
    orderRepository: OrderRepository
): AbstractNFTStorefrontSubscriber(collectionRepository, txManager, orderRepository) {

    override val name = "nft_storefront"
    override val contract = Contracts.NFT_STOREFRONT
}
