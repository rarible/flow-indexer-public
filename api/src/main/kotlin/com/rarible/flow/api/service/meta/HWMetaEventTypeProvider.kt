package com.rarible.flow.api.service.meta

import com.rarible.flow.Contracts
import com.rarible.flow.api.config.ApiProperties
import com.rarible.flow.core.domain.ItemId
import org.springframework.stereotype.Component

@Component
@Suppress("SameParameterValue")
class HWMetaEventTypeProvider(properties: ApiProperties) : MetaEventTypeProvider {

    private val chainId = properties.chainId

    override fun getMetaEventType(itemId: ItemId): MetaEventType? {
        return when (itemId.contract) {
            Contracts.HW_GARAGE_PACK.fqn(chainId) -> {
                getPackMetadataEvent(Contracts.HW_GARAGE_PM)
            }

            Contracts.HW_GARAGE_CARD.fqn(chainId) -> {
                getCardMetadataEvent(Contracts.HW_GARAGE_PM)
            }

            Contracts.HW_GARAGE_PACK_V2.fqn(chainId) -> {
                getAdminMintPackMetadataEvent(Contracts.HW_GARAGE_PM_V2)
            }
            Contracts.HW_GARAGE_CARD_V2.fqn(chainId) -> {
                getAdminMintCardMetadataEvent(Contracts.HW_GARAGE_PM_V2)
            }
            Contracts.RARIBLE_GARAGE_PACK.fqn(chainId) -> {
                getPackMetadataEvent(Contracts.RARIBLE_GARAGE_PM)
            }
            Contracts.RARIBLE_GARAGE_CARD.fqn(chainId) -> {
                getCardMetadataEvent(Contracts.RARIBLE_GARAGE_PM)
            }

            Contracts.RARIBLE_GARAGE_PACK_V2.fqn(chainId) -> {
                getAdminMintPackMetadataEvent(Contracts.RARIBLE_GARAGE_PM_V2)
            }

            Contracts.RARIBLE_GARAGE_CARD_V2.fqn(chainId) -> {
                getAdminMintCardMetadataEvent(Contracts.RARIBLE_GARAGE_PM_V2)
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

    private fun getAdminMintPackMetadataEvent(pmContract: Contracts): MetaEventType {
        return MetaEventType("${pmContract.fqn(chainId)}.$ADMIN_MINT_PACK", PACK_V2_ID)
    }

    private fun getAdminMintCardMetadataEvent(pmContract: Contracts): MetaEventType {
        return MetaEventType("${pmContract.fqn(chainId)}.$ADMIN_MINT_CARD")
    }

    private companion object {

        const val PACK_V2_ID = "packID"

        const val UPDATE_PACK_EDITION_METADATA = "UpdatePackEditionMetadata"
        const val UPDATE_TOKEN_EDITION_METADATA = "UpdateTokenEditionMetadata"
        const val ADMIN_MINT_CARD = "AdminMintCard"
        const val ADMIN_MINT_PACK = "AdminMintPack"
    }
}