package com.rarible.flow.scanner.subscriber.nft

import com.rarible.flow.Contracts
import com.rarible.flow.scanner.subscriber.EnableBarbie
import org.springframework.stereotype.Component

@Component
@EnableBarbie
class BarbieCardSubscriber : NonFungibleTokenSubscriber() {

    override val name = "barbie_card"
    override val contract = Contracts.BARBIE_CARD
}

