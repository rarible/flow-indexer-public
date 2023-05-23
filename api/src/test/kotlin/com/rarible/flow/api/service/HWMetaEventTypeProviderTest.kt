package com.rarible.flow.api.service

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.api.randomApiProperties
import com.rarible.flow.api.service.meta.HWMetaEventTypeProvider
import com.rarible.flow.core.test.randomItemId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HWMetaEventTypeProviderTest {
    private val properties = randomApiProperties().copy(chainId = FlowChainId.TESTNET)
    private val provider = HWMetaEventTypeProvider(properties)

    @Test
    fun `provide - ok, pack`() {
        val itemId = randomItemId().copy(contract = "A.6f6702697b205c18.HWGaragePack")
        val eventType = provider.getMetaEventType(itemId)
        assertThat(eventType?.eventType).isEqualTo("A.6f6702697b205c18.HWGaragePM.UpdatePackEditionMetadata")
        assertThat(eventType?.id).isEqualTo("id")
    }

    @Test
    fun `provide - ok, card`() {
        val itemId = randomItemId().copy(contract = "A.6f6702697b205c18.HWGarageCard")
        val eventType = provider.getMetaEventType(itemId)
        assertThat(eventType?.eventType).isEqualTo("A.6f6702697b205c18.HWGaragePM.UpdateTokenEditionMetadata")
        assertThat(eventType?.id).isEqualTo("id")
    }

    @Test
    fun `provide - ok, pack V2`() {
        val itemId = randomItemId().copy(contract = "A.6f6702697b205c18.HWGaragePackV2")
        val eventType = provider.getMetaEventType(itemId)
        assertThat(eventType?.eventType).isEqualTo("A.6f6702697b205c18.HWGaragePMV2.AdminMintPack")
        assertThat(eventType?.id).isEqualTo("packID")
    }

    @Test
    fun `provide - ok, card V2`() {
        val itemId = randomItemId().copy(contract = "A.6f6702697b205c18.HWGarageCardV2")
        val eventType = provider.getMetaEventType(itemId)
        assertThat(eventType?.eventType).isEqualTo("A.6f6702697b205c18.HWGaragePMV2.AdminMintCard")
        assertThat(eventType?.id).isEqualTo("id")
    }
}