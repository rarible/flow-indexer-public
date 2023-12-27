package com.rarible.flow.scanner.subscriber.nft

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.Contracts
import com.rarible.flow.scanner.subscriber.EnableRaribleBarbiePack
import org.springframework.stereotype.Component

@Component
@EnableRaribleBarbiePack
class RaribleBarbiePackSubscriber(chainId: FlowChainId) : NonFungibleTokenSubscriber(chainId) {
    override val name = "rarible_barbie_pack"
    override val contract = Contracts.RARIBLE_BARBIE_PACK
}
