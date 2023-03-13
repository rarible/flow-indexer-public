package com.rarible.flow.scanner.activity.order.parser

import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.scanner.service.SupportedNftCollectionProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class NFTStorefrontCancelEventParserTest : BaseNFTStorefrontEventParserTest() {
    private val provider = mockk<SupportedNftCollectionProvider> {
        every { get() } returns setOf("A.80102bce1de42dc4.HWGaragePack")
    }
    private val parser = NftStorefrontCancelEventParser(currencyService, provider)

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