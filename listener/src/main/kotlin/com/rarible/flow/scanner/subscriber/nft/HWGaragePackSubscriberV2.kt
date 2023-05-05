package com.rarible.flow.scanner.subscriber.nft

import com.rarible.flow.Contracts

//@Component
class HWGaragePackSubscriberV2: NonFungibleTokenSubscriber() {
    override val name = "hw_pack_v2"
    override val contract = Contracts.HW_GARAGE_PACK_V2
}