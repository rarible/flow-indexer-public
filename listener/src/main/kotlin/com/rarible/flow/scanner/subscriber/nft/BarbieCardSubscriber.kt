package com.rarible.flow.scanner.subscriber.nft

import com.rarible.flow.Contracts
import com.rarible.flow.scanner.subscriber.EnableBarbieCard
import org.springframework.stereotype.Component

@Component
@EnableBarbieCard
class BarbieCardSubscriber : NonFungibleTokenSubscriber() {

    override val name = "barbie_card"
    override val contract = Contracts.BARBIE_CARD
}

