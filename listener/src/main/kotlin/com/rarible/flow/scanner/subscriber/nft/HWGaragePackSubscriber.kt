package com.rarible.flow.scanner.subscriber.nft

import com.rarible.flow.Contracts

//@Component
class HWGaragePackSubscriber: NonFungibleTokenSubscriber() {
    override val name = "hw_pack"
    override val contract = Contracts.HW_GARAGE_PACK
}

