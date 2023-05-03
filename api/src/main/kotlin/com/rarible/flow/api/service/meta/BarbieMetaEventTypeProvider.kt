package com.rarible.flow.api.service.meta

import com.rarible.flow.Contracts
import com.rarible.flow.api.config.ApiProperties
import com.rarible.flow.core.domain.ItemId
import org.springframework.stereotype.Component

@Component
@Suppress("SameParameterValue")
class BarbieMetaEventTypeProvider(properties: ApiProperties) : MetaEventTypeProvider {

    private val chainId = properties.chainId

    override fun getMetaEventType(itemId: ItemId): MetaEventType? {
        return when (itemId.contract) {
            Contracts.BARBIE_PACK.fqn(chainId) -> {
                getPackMetadataEvent(Contracts.BARBIE_PM)
            }

            Contracts.BARBIE_CARD.fqn(chainId) -> {
                getCardMetadataEvent(Contracts.BARBIE_PM)
            }

            else -> null
        }
    }

    private fun getPackMetadataEvent(pmContract: Contracts): MetaEventType {
        return MetaEventType("${pmContract.fqn(chainId)}.$UPDATE_PACK_EDITION_METADATA")
    }

    private fun getCardMetadataEvent(pmContract: Contracts): MetaEventType {
        return MetaEventType("${pmContract.fqn(chainId)}.$UPDATE_TOKEN_EDITION_METADATA")
    }

    data class Result(
        val eventType: String,
        val id: String = DEFAULT_ID,
    )

    private companion object {

        const val DEFAULT_ID = "id"

        const val UPDATE_PACK_EDITION_METADATA = "UpdatePackEditionMetadata"
        const val UPDATE_TOKEN_EDITION_METADATA = "UpdateTokenEditionMetadata"
    }
}