package com.rarible.flow.scanner.subscriber.nft

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.Contracts
import com.rarible.flow.scanner.model.NonFungibleTokenEventType
import com.rarible.flow.scanner.subscriber.EnableGamisodesToken
import org.springframework.stereotype.Component

@Component
@EnableGamisodesToken
class GamisodesTokenSubscriber(chainId: FlowChainId) : NonFungibleTokenSubscriber(chainId) {

    private val mintEventName = "NFTMinted"
    private val burnEventName = "NFTBurned"

    override val events = setOf(
        mintEventName,
        NonFungibleTokenEventType.DEPOSIT.eventName,
        NonFungibleTokenEventType.WITHDRAW.eventName,
        burnEventName
    )
    override val name = "gamisodes"
    override val contract = Contracts.GAMISODES

    override fun fromEventName(eventName: String) =
        when (eventName) {
            mintEventName -> NonFungibleTokenEventType.MINT
            burnEventName -> NonFungibleTokenEventType.BURN
            else -> super.fromEventName(eventName)
        }
}
