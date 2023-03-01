package com.rarible.flow.scanner.activity.order

import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.core.apm.withSpan
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.scanner.activity.order.parser.NFTStorefrontCancelEventParser
import com.rarible.flow.scanner.activity.order.parser.NFTStorefrontEventParser
import com.rarible.flow.scanner.activity.order.parser.NFTStorefrontPurchaseEventParser
import com.rarible.flow.scanner.activity.order.parser.NFTStorefrontV1ListingEventParser
import com.rarible.flow.scanner.activity.order.parser.NFTStorefrontV2ListingEventParser
import org.springframework.stereotype.Component

abstract class NFTStorefrontActivityMaker(
    private val parsers: List<NFTStorefrontEventParser<*>>
) : WithPaymentsActivityMaker() {

    override val contractName: String = Contracts.NFT_STOREFRONT.contractName

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
class NFTStorefrontV1ActivityMaker(
    listParser: NFTStorefrontV1ListingEventParser,
    cancelParser: NFTStorefrontCancelEventParser,
    purchaseParser: NFTStorefrontPurchaseEventParser
) : NFTStorefrontActivityMaker(
    listOf(cancelParser, listParser, purchaseParser)
)

@Component
class NFTStorefrontV2ActivityMaker(
    listParser: NFTStorefrontV2ListingEventParser,
    cancelParser: NFTStorefrontCancelEventParser,
    purchaseParser: NFTStorefrontPurchaseEventParser
) : NFTStorefrontActivityMaker(
    listOf(cancelParser, listParser, purchaseParser)
)


