package com.rarible.flow.scanner.subscriber.nft

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.Contracts
import com.rarible.flow.scanner.subscriber.EnableBarbieCard
import org.springframework.stereotype.Component

@Component
@EnableBarbieCard
class BarbieCardSubscriber(chainId: FlowChainId) : NonFungibleTokenSubscriber(chainId) {

    override val name = "barbie_card"
    override val contract = Contracts.BARBIE_CARD
}
