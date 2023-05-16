package com.rarible.flow.scanner.subscriber.nft

import com.rarible.flow.Contracts
import com.rarible.flow.scanner.subscriber.EnableBarbieToken
import org.springframework.stereotype.Component

@Component
@EnableBarbieToken
class BarbieTokenSubscriber : NonFungibleTokenSubscriber() {

    override val name = "barbie_token"
    override val contract = Contracts.BARBIE_TOKEN
}

