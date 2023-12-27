package com.rarible.flow.scanner.subscriber.nft

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.Contracts
import com.rarible.flow.scanner.subscriber.EnableBarbiePack
import org.springframework.stereotype.Component

@Component
@EnableBarbiePack
class BarbiePackSubscriber(chainId: FlowChainId) : NonFungibleTokenSubscriber(chainId) {

    override val name = "barbie_pack"
    override val contract = Contracts.BARBIE_PACK
}
