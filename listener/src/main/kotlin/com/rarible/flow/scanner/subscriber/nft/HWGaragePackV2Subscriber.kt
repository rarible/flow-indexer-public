package com.rarible.flow.scanner.subscriber.nft

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.Contracts
import org.springframework.stereotype.Component

@Component
class HWGaragePackV2Subscriber(chainId: FlowChainId) : NonFungibleTokenSubscriber(chainId) {
    override val name = "hw_pack_v2"
    override val contract = Contracts.HW_GARAGE_PACK_V2
}
