package com.rarible.flow.scanner.subscriber.nft

import com.rarible.flow.Contracts
import com.rarible.flow.scanner.subscriber.EnableRariblePackV2
import org.springframework.stereotype.Component

@Component
@EnableRariblePackV2
class RaribleGaragePackSubscriberV2: NonFungibleTokenSubscriber() {
    override val name = "rarible_pack_v2"
    override val contract = Contracts.RARIBLE_GARAGE_PACK_V2
}