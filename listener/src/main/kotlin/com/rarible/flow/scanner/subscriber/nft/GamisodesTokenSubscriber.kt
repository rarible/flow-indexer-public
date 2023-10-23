package com.rarible.flow.scanner.subscriber.nft

import com.rarible.flow.Contracts
import com.rarible.flow.scanner.subscriber.EnableGamisodesToken
import org.springframework.stereotype.Component

@Component
@EnableGamisodesToken
class GamisodesTokenSubscriber : NonFungibleTokenSubscriber() {

    override val name = "gamisodes"
    override val contract = Contracts.GAMISODES
}
