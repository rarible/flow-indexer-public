package com.rarible.flow.scanner.subscriber.nft

import com.rarible.flow.Contracts
import com.rarible.flow.scanner.model.NonFungibleTokenEventType
import com.rarible.flow.scanner.subscriber.EnableGamisodesToken
import org.springframework.stereotype.Component

@Component
@EnableGamisodesToken
class GamisodesTokenSubscriber : NonFungibleTokenSubscriber() {

    private val mintEventName = "NFTMinted"

    override val events = super.events - NonFungibleTokenEventType.MINT.eventName + mintEventName
    override val name = "gamisodes"
    override val contract = Contracts.GAMISODES

    override fun fromEventName(eventName: String) =
        when (eventName) {
            mintEventName -> NonFungibleTokenEventType.MINT
            else -> super.fromEventName(eventName)
        }
}
