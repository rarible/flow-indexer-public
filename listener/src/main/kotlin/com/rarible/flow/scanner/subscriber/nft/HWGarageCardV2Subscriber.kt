package com.rarible.flow.scanner.subscriber.nft

import com.rarible.flow.Contracts
import org.springframework.stereotype.Component

@Component
class HWGarageCardV2Subscriber: NonFungibleTokenSubscriber() {
    override val name = "hw_card_v2"
    override val contract = Contracts.HW_GARAGE_CARD_V2
}

