package com.rarible.flow.scanner.subscriber.nft

import com.rarible.flow.Contracts
import com.rarible.flow.scanner.subscriber.EnableHWOtherCard
import org.springframework.stereotype.Component

@Component
@EnableHWOtherCard
class HWOtherGarageCardSubscriber : NonFungibleTokenSubscriber() {
    override val name = "hw_other_card"
    override val contract = Contracts.HW_OTHER_GARAGE_CARD
}