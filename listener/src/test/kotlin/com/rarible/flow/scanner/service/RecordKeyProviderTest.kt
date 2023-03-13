package com.rarible.flow.scanner.service

import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.scanner.BaseIntegrationTest
import com.rarible.flow.scanner.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class RecordKeyProviderTest : BaseIntegrationTest() {
    @Autowired
    private lateinit var provider: RecordKeyProvider

    @Test
    fun `get - ok, mint`() {
        val itemId = provider.getRecordKey(getMintFlowLogEvent())
        assertThat(itemId).isEqualTo(ItemId("A.80102bce1de42dc4.HWGaragePack", 2).toString())
    }

    @Test
    fun `get - ok, burn`() {
        val itemId = provider.getRecordKey(getBurnFlowLogEvent())
        assertThat(itemId).isEqualTo(ItemId("A.80102bce1de42dc4.HWGaragePack", 2).toString())
    }

    @Test
    fun `get - ok, deposit`() {
        val itemId = provider.getRecordKey(getDepositFlowLogEvent())
        assertThat(itemId).isEqualTo(ItemId("A.80102bce1de42dc4.HWGaragePack", 2).toString())
    }

    @Test
    fun `get - ok, withdraw`() {
        val itemId = provider.getRecordKey(getWithdrawFlowLogEvent())
        assertThat(itemId).isEqualTo(ItemId("A.80102bce1de42dc4.HWGaragePack", 2).toString())
    }

    @Test
    fun `get - ok, listing`() {
        val itemId = provider.getRecordKey(getStorefrontV2ListingFlowLogEvent())
        assertThat(itemId).isEqualTo(ItemId("A.80102bce1de42dc4.HWGaragePack", 2).toString())
    }

    @Test
    fun `get - ok, purchase`() {
        val itemId = provider.getRecordKey(getStorefrontV2PurchaseFlowLogEvent())
        assertThat(itemId).isEqualTo(ItemId("A.80102bce1de42dc4.HWGaragePack", 2).toString())
    }
}