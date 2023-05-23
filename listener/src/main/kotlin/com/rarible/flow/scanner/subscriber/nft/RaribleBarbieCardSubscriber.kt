package com.rarible.flow.scanner.subscriber.nft

import com.rarible.flow.Contracts
import com.rarible.flow.scanner.subscriber.EnableRaribleBarbieCard
import org.springframework.stereotype.Component

@Component
@EnableRaribleBarbieCard
class RaribleBarbieCardSubscriber : NonFungibleTokenSubscriber() {
    override val name = "rarible_barbie_card"
    override val contract = Contracts.RARIBLE_BARBIE_CARD
}