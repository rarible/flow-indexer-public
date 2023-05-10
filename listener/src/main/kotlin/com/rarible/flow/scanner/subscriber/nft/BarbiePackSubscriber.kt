package com.rarible.flow.scanner.subscriber.nft

import com.rarible.flow.Contracts
import com.rarible.flow.scanner.subscriber.EnableBarbie
import org.springframework.stereotype.Component

@Component
@EnableBarbie
class BarbiePackSubscriber : NonFungibleTokenSubscriber() {

    override val name = "barbie_pack"
    override val contract = Contracts.BARBIE_PACK
}

