package com.rarible.flow.api.royalty.provider

import com.rarible.flow.Contracts
import com.rarible.flow.core.config.AppProperties
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order
class ContractsItemRoyaltyProvider(
    private val appProperties: AppProperties
): ItemRoyaltyProvider {
    override fun isSupported(itemId: ItemId): Boolean = Contracts.values().any { it.supports(itemId) }

    override suspend fun getRoyalties(item: Item): List<Royalty> {
        return Contracts.values()
            .find { it.supports(item.id) }!!
            .staticRoyalties(appProperties.chainId).map { Royalty(it) }
    }
}
