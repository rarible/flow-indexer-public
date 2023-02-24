package com.rarible.flow.api.service

import com.rarible.flow.Contracts
import com.rarible.flow.api.config.ApiProperties
import com.rarible.flow.core.domain.ItemId
import org.springframework.stereotype.Component

@Component
@Suppress("SameParameterValue")
class HWMetaEventTypeProvider(properties: ApiProperties) {

    private val chainId = properties.chainId

    fun getMetaEventType(itemId: ItemId): String? {
        return when (itemId.contract) {
            Contracts.HW_GARAGE_PACK.fqn(chainId) -> {
                getPackMetadataEvent(Contracts.HW_GARAGE_PM)
            }
            Contracts.HW_GARAGE_CARD.fqn(chainId) -> {
                getCardMetadataEvent(Contracts.HW_GARAGE_PM)
            }
            else -> null
        }
    }

    private fun getPackMetadataEvent(pmContract: Contracts): String {
        return "${pmContract.fqn(chainId)}.$UPDATE_PACK_EDITION_METADATA"
    }

    private fun getCardMetadataEvent(pmContract: Contracts): String {
        return "${pmContract.fqn(chainId)}.$UPDATE_TOKEN_EDITION_METADATA"
    }

    private companion object {
        const val UPDATE_PACK_EDITION_METADATA = "UpdatePackEditionMetadata"
        const val UPDATE_TOKEN_EDITION_METADATA = "UpdateTokenEditionMetadata"
    }
}