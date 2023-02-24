package com.rarible.flow.api.service

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.api.randomApiProperties
import com.rarible.flow.core.test.randomItemId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HWMetaEventTypeProviderTest {
    private val properties = randomApiProperties().copy(chainId = FlowChainId.TESTNET)
    private val provider = HWMetaEventTypeProvider(properties)

    @Test
    fun `provide - ok, pack`() {
        val itemId = randomItemId().copy(contract = "A.9f36754d9b38f155.HWGaragePack")
        val eventType = provider.getMetaEventType(itemId)
        assertThat(eventType).isEqualTo("A.9f36754d9b38f155.HWGaragePM.UpdatePackEditionMetadata")
    }

    @Test
    fun `provide - ok, card`() {
        val itemId = randomItemId().copy(contract = "A.9f36754d9b38f155.HWGarageCard")
        val eventType = provider.getMetaEventType(itemId)
        assertThat(eventType).isEqualTo("A.9f36754d9b38f155.HWGaragePM.UpdateTokenEditionMetadata")
    }
}