package com.rarible.flow.scanner.subscriber.nft

import com.rarible.flow.Contracts

//@Component
class HWGarageCardSubscriberV2: NonFungibleTokenSubscriber() {
    override val name = "hw_card_v2"
    override val contract = Contracts.HW_GARAGE_CARD_V2
}