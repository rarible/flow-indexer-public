package com.rarible.flow.scanner.subscriber.nft

import com.rarible.flow.Contracts
import com.rarible.flow.scanner.subscriber.EnableRaribleCard
import org.springframework.stereotype.Component

@Component
@EnableRaribleCard
class RaribleGarageCardSubscriber : NonFungibleTokenSubscriber() {
    override val name = "rarible_card"
    override val contract = Contracts.RARIBLE_GARAGE_CARD
}
