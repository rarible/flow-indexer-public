package com.rarible.flow.scanner.subscriber.nft

import com.rarible.flow.Contracts
import org.springframework.stereotype.Component

@Component
class HWGarageTokenV2Subscriber : NonFungibleTokenSubscriber() {
    override val name = "hw_token_v2"
    override val contract = Contracts.HW_GARAGE_TOKEN_V2
}
