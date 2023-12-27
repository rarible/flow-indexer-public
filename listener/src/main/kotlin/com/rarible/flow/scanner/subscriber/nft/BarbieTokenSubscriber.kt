package com.rarible.flow.scanner.subscriber.nft

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.Contracts
import com.rarible.flow.scanner.subscriber.EnableBarbieToken
import org.springframework.stereotype.Component

@Component
@EnableBarbieToken
class BarbieTokenSubscriber(chainId: FlowChainId) : NonFungibleTokenSubscriber(chainId) {

    override val name = "barbie_token"
    override val contract = Contracts.BARBIE_TOKEN
}
