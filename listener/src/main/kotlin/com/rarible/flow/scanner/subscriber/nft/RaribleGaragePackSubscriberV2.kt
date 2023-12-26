package com.rarible.flow.scanner.subscriber.nft

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.Contracts
import com.rarible.flow.scanner.subscriber.EnableRariblePackV2
import org.springframework.stereotype.Component

@Component
@EnableRariblePackV2
class RaribleGaragePackSubscriberV2(chainId: FlowChainId) : NonFungibleTokenSubscriber(chainId) {
    override val name = "rarible_pack_v2"
    override val contract = Contracts.RARIBLE_GARAGE_PACK_V2
}
