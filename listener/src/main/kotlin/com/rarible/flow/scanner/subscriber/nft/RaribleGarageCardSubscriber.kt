package com.rarible.flow.scanner.subscriber.nft

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.Contracts
import com.rarible.flow.scanner.subscriber.EnableRaribleCard
import org.springframework.stereotype.Component

@Component
@EnableRaribleCard
class RaribleGarageCardSubscriber(chainId: FlowChainId) : NonFungibleTokenSubscriber(chainId) {
    override val name = "rarible_card"
    override val contract = Contracts.RARIBLE_GARAGE_CARD
}
