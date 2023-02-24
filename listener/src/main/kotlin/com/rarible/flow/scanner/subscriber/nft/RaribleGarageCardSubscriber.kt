package com.rarible.flow.scanner.subscriber.nft

import com.rarible.flow.Contracts
import com.rarible.flow.scanner.subscriber.NonFungibleTokenSubscriber
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Component

@Component
@ConditionalOnExpression("false")
class RaribleGarageCardSubscriber: NonFungibleTokenSubscriber() {
    override val name = "rarible_card"
    override val contract = Contracts.RARIBLE_GARAGE_CARD
}