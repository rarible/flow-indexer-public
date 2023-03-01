package com.rarible.flow.scanner.subscriber.nft

import com.rarible.flow.Contracts
import org.springframework.stereotype.Component

@Component
class RaribleGaragePackSubscriber: NonFungibleTokenSubscriber() {
    override val name = "rarible_pack"
    override val contract = Contracts.RARIBLE_GARAGE_PACK
}