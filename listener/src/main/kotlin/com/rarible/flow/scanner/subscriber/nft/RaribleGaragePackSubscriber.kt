package com.rarible.flow.scanner.subscriber.nft

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.Contracts
import com.rarible.flow.scanner.subscriber.EnableRariblePack
import org.springframework.stereotype.Component

@Component
@EnableRariblePack
class RaribleGaragePackSubscriber(chainId: FlowChainId) : NonFungibleTokenSubscriber(chainId) {
    override val name = "rarible_pack"
    override val contract = Contracts.RARIBLE_GARAGE_PACK
}
