package com.rarible.flow.scanner.subscriber.nft

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.Contracts
import org.springframework.stereotype.Component

@Component
class HWGarageCardV2Subscriber(chainId: FlowChainId) : NonFungibleTokenSubscriber(chainId) {
    override val name = "hw_card_v2"
    override val contract = Contracts.HW_GARAGE_CARD_V2
}
