package com.rarible.flow.scanner.subscriber.nft

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.Contracts
import org.springframework.stereotype.Component

@Component
class HWGaragePackSubscriber(chainId: FlowChainId) : NonFungibleTokenSubscriber(chainId) {
    override val name = "hw_pack"
    override val contract = Contracts.HW_GARAGE_PACK
}
