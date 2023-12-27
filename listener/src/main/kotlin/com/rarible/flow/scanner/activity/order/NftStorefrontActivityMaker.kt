package com.rarible.flow.scanner.activity.order

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.scanner.activity.order.parser.NftStorefrontCancelEventParser
import com.rarible.flow.scanner.activity.order.parser.NftStorefrontV1ListingEventParser
import com.rarible.flow.scanner.activity.order.parser.NftStorefrontV1PurchaseEventParser
import com.rarible.flow.scanner.activity.order.parser.NftStorefrontV2ListingEventParser
import com.rarible.flow.scanner.activity.order.parser.NftStorefrontV2PurchaseEventParser
import org.springframework.stereotype.Component

abstract class NftStorefrontActivityMaker(
    private val parsers: List<NftStorefrontEventParser<*>>,
    override val contractName: String,
    chainId: FlowChainId
) : WithPaymentsActivityMaker(chainId) {

    override suspend fun activities(events: List<FlowLogEvent>): Map<FlowLog, BaseActivity> {
        return parsers
            .map { parser -> parser.parseActivities(events) }
            .fold(mutableMapOf()) { acc, next ->
                acc.putAll(next)
                acc
            }
    }

    override fun getItemId(event: FlowLogEvent): ItemId? {
        val iterator = parsers.iterator()
        while (iterator.hasNext()) {
            return iterator.next().getItemId(event) ?: continue
        }
        return null
    }
}

@Component
class NftStorefrontV1ActivityMaker(
    listParser: NftStorefrontV1ListingEventParser,
    purchaseParser: NftStorefrontV1PurchaseEventParser,
    cancelParser: NftStorefrontCancelEventParser,
    chainId: FlowChainId,
) : NftStorefrontActivityMaker(
    contractName = Contracts.NFT_STOREFRONT.contractName,
    parsers = listOf(cancelParser, listParser, purchaseParser),
    chainId = chainId,
)

@Component
class NftStorefrontV2ActivityMaker(
    listParser: NftStorefrontV2ListingEventParser,
    purchaseParser: NftStorefrontV2PurchaseEventParser,
    cancelParser: NftStorefrontCancelEventParser,
    chainId: FlowChainId,
) : NftStorefrontActivityMaker(
    contractName = Contracts.NFT_STOREFRONT_V2.contractName,
    parsers = listOf(cancelParser, listParser, purchaseParser),
    chainId = chainId,
)
