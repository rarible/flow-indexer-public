package com.rarible.flow.scanner.subscriber.nft

import com.rarible.flow.Contracts
import com.rarible.flow.scanner.subscriber.EnableRaribleCardV2
import org.springframework.stereotype.Component

@Component
@EnableRaribleCardV2
class RaribleGarageCardSubscriberV2: NonFungibleTokenSubscriber() {
    override val name = "rarible_card_v2"
    override val contract = Contracts.RARIBLE_GARAGE_CARD_V2
}