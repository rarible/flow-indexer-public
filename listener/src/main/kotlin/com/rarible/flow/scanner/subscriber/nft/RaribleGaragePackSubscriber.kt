package com.rarible.flow.scanner.subscriber.nft

import com.rarible.flow.Contracts
import com.rarible.flow.scanner.subscriber.NonFungibleTokenSubscriber
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Component

@Component
@ConditionalOnExpression("false")
class RaribleGaragePackSubscriber: NonFungibleTokenSubscriber() {
    override val name = "rarible_pack"
    override val contract = Contracts.RARIBLE_GARAGE_PACK
}