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

            Contracts.BARBIE_TOKEN.fqn(chainId) -> {
                getTokenMetadataEvent(Contracts.BARBIE_PM)
            }

            else -> null
        }
    }

    private fun getPackMetadataEvent(pmContract: Contracts): MetaEventType {
        return MetaEventType("${pmContract.fqn(chainId)}.$ADMIN_MINT_PACK", "packID")
    }

    private fun getCardMetadataEvent(pmContract: Contracts): MetaEventType {
        return MetaEventType("${pmContract.fqn(chainId)}.$ADMIN_MINT_CARD")
    }

    private fun getTokenMetadataEvent(pmContract: Contracts): MetaEventType {
        return MetaEventType("${pmContract.fqn(chainId)}.$ADMIN_MINT_TOKEN")
    }


    private companion object {
        const val ADMIN_MINT_CARD = "AdminMintCard"
        const val ADMIN_MINT_PACK = "AdminMintPack"
        const val ADMIN_MINT_TOKEN = "AdminMintToken"
    }
}