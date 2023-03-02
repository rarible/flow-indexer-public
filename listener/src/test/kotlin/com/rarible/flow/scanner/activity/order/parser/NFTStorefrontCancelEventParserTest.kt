package com.rarible.flow.scanner.activity.order.parser

import com.rarible.flow.core.domain.FlowLogType
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class NFTStorefrontCancelEventParserTest : BaseNFTStorefrontEventParserTest() {
    private val parser = NFTStorefrontCancelEventParser(currencyService)

    @Test
    fun `parse - ok`() = runBlocking<Unit> {
        val flowLogEvent = getFlowLogEvent(
            json = "/json/nft_storefront_v2_cancel.json",
            type = FlowLogType.LISTING_COMPLETED)

        val activities = parser.parseActivities(listOf(flowLogEvent))
        Assertions.assertThat(activities).hasSize(1)

        val listing = activities.entries.single().value
        Assertions.assertThat(listing.hash).isEqualTo("910674636")
        Assertions.assertThat(listing.timestamp).isEqualTo(flowLogEvent.log.timestamp)
    }
}