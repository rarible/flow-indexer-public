package com.rarible.flow.scanner.subscriber.nft

import com.rarible.flow.Contracts
import org.springframework.stereotype.Component

@Component
class BarbieTokenSubscriber : NonFungibleTokenSubscriber() {

    override val name = "barbie_token"
    override val contract = Contracts.BARBIE_TOKEN
}

