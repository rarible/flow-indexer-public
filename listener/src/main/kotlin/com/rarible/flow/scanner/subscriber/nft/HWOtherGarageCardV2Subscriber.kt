package com.rarible.flow.scanner.subscriber.nft

import com.rarible.flow.Contracts
import com.rarible.flow.scanner.subscriber.EnableHWOtherCardV2
import org.springframework.stereotype.Component

@Component
@EnableHWOtherCardV2
class HWOtherGarageCardV2Subscriber: NonFungibleTokenSubscriber() {
    override val name = "hw_other_card_v2"
    override val contract = Contracts.HW_OTHER_GARAGE_CARD_V2
}