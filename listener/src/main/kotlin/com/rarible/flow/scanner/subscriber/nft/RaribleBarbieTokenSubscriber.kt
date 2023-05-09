package com.rarible.flow.scanner.subscriber.nft

import com.rarible.flow.Contracts
import com.rarible.flow.scanner.subscriber.EnableRaribleBarbieToken
import org.springframework.stereotype.Component

@Component
@EnableRaribleBarbieToken
class RaribleBarbieTokenSubscriber : NonFungibleTokenSubscriber() {
    override val name = "rarible_barbie_token"
    override val contract = Contracts.RARIBLE_BARBIE_TOKEN
}