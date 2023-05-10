package com.rarible.flow.scanner.subscriber.nft

import com.rarible.flow.Contracts
import com.rarible.flow.scanner.subscriber.EnableBarbie
import org.springframework.stereotype.Component

@Component
@EnableBarbie
class BarbieTokenSubscriber : NonFungibleTokenSubscriber() {

    override val name = "barbie_token"
    override val contract = Contracts.BARBIE_TOKEN
}

