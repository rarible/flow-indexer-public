package com.rarible.flow.scanner.subscriber.nft

import com.rarible.flow.Contracts
import com.rarible.flow.scanner.subscriber.EnableRariblePack
import org.springframework.stereotype.Component

@Component
@EnableRariblePack
class RaribleGaragePackSubscriberV2: NonFungibleTokenSubscriber() {
    override val name = "rarible_pack_v2"
    override val contract = Contracts.RARIBLE_GARAGE_PACK_V2
}