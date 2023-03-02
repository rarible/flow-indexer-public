package com.rarible.flow.scanner.activity.order.parser

import com.rarible.flow.core.domain.FlowAssetFungible
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.flow.core.domain.FlowLogType
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class NFTStorefrontV2ListingEventParserTest : BaseNFTStorefrontEventParserTest() {
    private val parser = NFTStorefrontV2ListingEventParser(currencyService)

    @Test
    fun `parse - ok`() = runBlocking<Unit> {
        val flowLogEvent = getFlowLogEvent(
            json = "/json/nft_storefront_v2_listing.json",
            type = FlowLogType.LISTING_AVAILABLE)

        val expectedNftAsset = FlowAssetNFT(
            contract = "A.eee6bdee2b2bdfc8.Basketballs",
            value = BigDecimal.ONE,
            tokenId = 2,
        )
        val expectedCurrencyAsset = FlowAssetFungible(
            contract = "A.ead892083b3e2c6c.DapperUtilityCoin",
            value = BigDecimal("10.00000000"),
        )
        coEvery {
            currencyService.getUsdRate(expectedCurrencyAsset.contract, flowLogEvent.log.timestamp)
        } returns BigDecimal("0.1")

        val activities = parser.parseActivities(listOf(flowLogEvent))
        assertThat(activities).hasSize(1)

        val listing = activities.entries.single().value
        assertThat(listing.hash).isEqualTo("910213870")
        assertThat(listing.maker).isEqualTo("0x8500afb0163a33c8")
        assertThat(listing.make).isEqualTo(expectedNftAsset)
        assertThat(listing.take).isEqualTo(expectedCurrencyAsset)
        assertThat(listing.price).isEqualTo(expectedCurrencyAsset.value)
        assertThat(listing.contract).isEqualTo(expectedNftAsset.contract)
        assertThat(listing.tokenId).isEqualTo(expectedNftAsset.tokenId)
        assertThat(listing.timestamp).isEqualTo(flowLogEvent.log.timestamp)
        assertThat(listing.priceUsd).isEqualTo(BigDecimal("1.000000000"))
    }
}