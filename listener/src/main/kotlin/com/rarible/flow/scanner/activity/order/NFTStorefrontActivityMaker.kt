package com.rarible.flow.scanner.activity.order

import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.core.apm.withSpan
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.scanner.activity.order.parser.NftStorefrontCancelEventParser
import com.rarible.flow.scanner.activity.order.parser.NftStorefrontV1ListingEventParser
import com.rarible.flow.scanner.activity.order.parser.NftStorefrontV1PurchaseEventParser
import com.rarible.flow.scanner.activity.order.parser.NftStorefrontV2ListingEventParser
import com.rarible.flow.scanner.activity.order.parser.NftStorefrontV2PurchaseEventParser
import org.springframework.stereotype.Component

abstract class NftStorefrontActivityMaker(
    private val parsers: List<NftStorefrontEventParser<*>>,
    override val contractName: String
) : WithPaymentsActivityMaker() {

    override suspend fun activities(events: List<FlowLogEvent>): Map<FlowLog, BaseActivity> {
        return withSpan("generateOrderActivities", "event") {
            parsers
                .map { parser -> parser.parseActivities(events) }
                .fold(mutableMapOf()) { acc, next ->
                    acc.putAll(next)
                    acc
                }
        }
    }
}

@Component
class NftStorefrontV1ActivityMaker(
    listParser: NftStorefrontV1ListingEventParser,
    purchaseParser: NftStorefrontV1PurchaseEventParser,
    cancelParser: NftStorefrontCancelEventParser,
) : NftStorefrontActivityMaker(
    contractName = Contracts.NFT_STOREFRONT.contractName,
    parsers = listOf(cancelParser, listParser, purchaseParser)
)

@Component
class NftStorefrontV2ActivityMaker(
    listParser: NftStorefrontV2ListingEventParser,
    purchaseParser: NftStorefrontV2PurchaseEventParser,
    cancelParser: NftStorefrontCancelEventParser,
) : NftStorefrontActivityMaker(
    contractName = Contracts.NFT_STOREFRONT_V2.contractName,
    parsers = listOf(cancelParser, listParser, purchaseParser)
)

