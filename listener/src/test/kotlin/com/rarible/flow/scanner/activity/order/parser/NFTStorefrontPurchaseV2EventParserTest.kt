package com.rarible.flow.scanner.activity.order.parser

import com.nftco.flow.sdk.FlowId
import com.rarible.flow.core.domain.FlowAssetFungible
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.test.randomFlowTransactionResult
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.service.SupportedNftCollectionProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class NFTStorefrontPurchaseV2EventParserTest : BaseNFTStorefrontEventParserTest() {
    private val txManager = mockk<TxManager>()
    private val provider = mockk<SupportedNftCollectionProvider> {
        every { get() } returns setOf("A.eee6bdee2b2bdfc8.Basketballs")
    }
    private val parser = NftStorefrontV2PurchaseEventParser(
        txManager = txManager,
        currencyService = currencyService,
        supportedNftCollectionProvider = provider
    )

    @Test
    fun `parse - ok`() = runBlocking<Unit> {
        val purchaseLogEvent = getFlowLogEvent(
            json = "/json/nft_storefront_v2_purchase.json",
            type = FlowLogType.LISTING_COMPLETED
        )

        val depositLogEvent = getFlowEvent("/json/nft_deposit.json")
        val withdrawLogEvent = getFlowEvent("/json/nft_withdraw.json")

        val expectedNftAsset = FlowAssetNFT(
            contract = "A.80102bce1de42dc4.HWGaragePack",
            value = BigDecimal.ONE,
            tokenId = 2,
        )
        val expectedCurrencyAsset = FlowAssetFungible(
            contract = "A.ead892083b3e2c6c.DapperUtilityCoin",
            value = BigDecimal("10.00000000"),
        )

        coEvery {
            txManager.getTransactionEvents(
                blockHeight = purchaseLogEvent.log.blockHeight,
                transactionId = FlowId(purchaseLogEvent.log.transactionHash)
            )
        } returns randomFlowTransactionResult(events = listOf(withdrawLogEvent, depositLogEvent))

        coEvery {
            currencyService.getUsdRate(expectedCurrencyAsset.contract, purchaseLogEvent.log.timestamp)
        } returns BigDecimal("0.1")

        val activities = parser.parseActivities(listOf(purchaseLogEvent))
        Assertions.assertThat(activities).hasSize(1)

        val purchase = activities.entries.single().value
        Assertions.assertThat(purchase.hash).isEqualTo("910213780")
        Assertions.assertThat(purchase.left.asset).isEqualTo(expectedNftAsset)
        Assertions.assertThat(purchase.left.maker).isEqualTo("0x987ef81a43bb4780")
        Assertions.assertThat(purchase.right.asset).isEqualTo(expectedCurrencyAsset)
        Assertions.assertThat(purchase.right.maker).isEqualTo("0x8500afb0163a33c8")
        Assertions.assertThat(purchase.price).isEqualTo(expectedCurrencyAsset.value)
        Assertions.assertThat(purchase.contract).isEqualTo(expectedNftAsset.contract)
        Assertions.assertThat(purchase.tokenId).isEqualTo(expectedNftAsset.tokenId)
        Assertions.assertThat(purchase.timestamp).isEqualTo(purchaseLogEvent.log.timestamp)
        Assertions.assertThat(purchase.priceUsd).isEqualTo(BigDecimal("1.000000000"))
        Assertions.assertThat(purchase.payments).isEmpty()
    }

    @Test
    fun `parse - ok,with fee`() = runBlocking<Unit> {
        val purchaseLogEvent = getFlowLogEvent(
            json = "/json/nft_storefront_v2_purchase_with_fee.json",
            type = FlowLogType.LISTING_COMPLETED
        )

        val depositLogEvent = getFlowEvent("/json/nft_deposit.json")
        val withdrawLogEvent = getFlowEvent("/json/nft_withdraw.json")
        val expectedRate = BigDecimal("0.1")

        coEvery {
            txManager.getTransactionEvents(
                blockHeight = any(),
                transactionId = any()
            )
        } returns randomFlowTransactionResult(events = listOf(withdrawLogEvent, depositLogEvent))

        coEvery {
            currencyService.getUsdRate("A.7e60df042a9c0868.FlowToken", purchaseLogEvent.log.timestamp)
        } returns BigDecimal("0.1")

        val activities = parser.parseActivities(listOf(purchaseLogEvent))
        Assertions.assertThat(activities).hasSize(1)

        val purchase = activities.entries.single().value
        Assertions.assertThat(purchase.estimatedFee?.receivers?.single()).isEqualTo("0x4895ce5fb8a40f47")
        Assertions.assertThat(purchase.estimatedFee?.amount).isEqualTo(BigDecimal("0.50000000"))
    }
}
