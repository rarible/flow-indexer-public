package com.rarible.flow.scanner.subscriber.nft

import com.rarible.flow.Contracts
import org.springframework.stereotype.Component

@Component
class HWGaragePackV2Subscriber : NonFungibleTokenSubscriber() {
    override val name = "hw_pack_v2"
    override val contract = Contracts.HW_GARAGE_PACK_V2
}
