package com.rarible.flow.scanner.subscriber.nft

import com.rarible.flow.Contracts
import com.rarible.flow.scanner.subscriber.EnableRariblePack
import org.springframework.stereotype.Component

@Component
@EnableRariblePack
class RaribleGaragePackSubscriber : NonFungibleTokenSubscriber() {
    override val name = "rarible_pack"
    override val contract = Contracts.RARIBLE_GARAGE_PACK
}
