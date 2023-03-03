package com.rarible.flow.scanner.activity.order.parser

import com.nftco.flow.sdk.FlowEvent
import com.nftco.flow.sdk.FlowId
import com.nftco.flow.sdk.FlowTransactionResult
import com.rarible.flow.core.domain.FlowAssetFungible
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowNftOrderActivityList
import com.rarible.flow.core.domain.FlowNftOrderActivitySell
import com.rarible.flow.core.domain.OrderActivityMatchSide
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.event.EventMessage
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.model.NonFungibleTokenEventType
import com.rarible.flow.scanner.service.CurrencyService
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class NFTStorefrontV2PurchaseEventParser(
    currencyService: CurrencyService,
    private val txManager: TxManager,
) : NFTStorefrontPurchaseEventParser(currencyService) {

    override suspend fun parseActivity(logEvent: FlowLogEvent): FlowNftOrderActivitySell {
        val listing = delegate.safeParseActivity(logEvent)
        val transactionEvents = txManager.getTransactionEvents(
            blockHeight = logEvent.log.blockHeight,
            transactionId = FlowId(logEvent.log.transactionHash)
        )
        val nft = listing.make as FlowAssetNFT
        val currency = listing.take
        val transfer = getNftTokenTransfer(nft, transactionEvents)

        val seller = transfer.from
        val buyer = transfer.to

        return FlowNftOrderActivitySell(
            price = listing.price,
            priceUsd = listing.priceUsd,
            tokenId = listing.tokenId,
            contract = listing.contract,
            hash = listing.hash,
            left = OrderActivityMatchSide(
                maker = seller,
                asset = FlowAssetNFT(
                    contract = nft.contract,
                    tokenId = nft.tokenId,
                    value = BigDecimal.ONE
                )
            ),
            right = OrderActivityMatchSide(
                maker = buyer,
                asset = FlowAssetFungible(
                    contract = currency.contract,
                    value = listing.price
                )
            ),
            timestamp = logEvent.log.timestamp,
            payments = emptyList()
        )
    }

    private val delegate = object : NFTStorefrontV2ListingEventParser(currencyService) {
        override fun getMaker(event: EventMessage): String {
            return ""
        }

        override suspend fun safeParseActivity(logEvent: FlowLogEvent): FlowNftOrderActivityList {
            return super.safeParseActivity(logEvent) ?: error("Unexpected null")
        }
    }

    internal data class NftTransfer(
        val from: String,
        val to: String
    )

    private fun getNftTokenTransfer(
        asset: FlowAssetNFT,
        events: FlowTransactionResult
    ): NftTransfer {
        val from = events.events.single {
            it.id == NonFungibleTokenEventType.WITHDRAW.full(asset.contract) &&
            getTokenId(it) == asset.tokenId
        }.let { getOptionalAddress(it, "from")!! }

        val to = events.events.single {
            it.id == NonFungibleTokenEventType.DEPOSIT.full(asset.contract) &&
            getTokenId(it) == asset.tokenId
        }.let { getOptionalAddress(it, "to")!! }

        return NftTransfer(from = from, to = to)
    }

    protected fun getOptionalAddress(event: FlowEvent, name: String): String? {
        return cadenceParser.optional(event.getField(name)!!) { field -> address(field) }
    }

    private fun getTokenId(event: FlowEvent): TokenId {
        return cadenceParser.long(event.getField("id")!!)
    }
}