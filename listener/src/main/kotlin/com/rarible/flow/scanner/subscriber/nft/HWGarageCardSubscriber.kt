package com.rarible.flow.scanner.subscriber.nft

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.Contracts
import org.springframework.stereotype.Component

@Component
class HWGarageCardSubscriber(chainId: FlowChainId) : NonFungibleTokenSubscriber(chainId) {
    override val name = "hw_card"
    override val contract = Contracts.HW_GARAGE_CARD
}
