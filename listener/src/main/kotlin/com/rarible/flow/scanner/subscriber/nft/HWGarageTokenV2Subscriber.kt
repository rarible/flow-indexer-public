package com.rarible.flow.scanner.subscriber.nft

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.Contracts
import org.springframework.stereotype.Component

@Component
class HWGarageTokenV2Subscriber(chainId: FlowChainId) : NonFungibleTokenSubscriber(chainId) {
    override val name = "hw_token_v2"
    override val contract = Contracts.HW_GARAGE_TOKEN_V2
}
