package com.rarible.flow.api.royaltyprovider

import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import java.math.BigDecimal

// todo refactor com.rarible.flow.core.domain.Part::fee Double -> BigDecimal, then replace
data class Royalty(val address: String, val fee: BigDecimal)

interface ItemRoyaltyProvider {
    fun isSupported(itemId: ItemId): Boolean
    suspend fun getRoyalty(item: Item): List<Royalty>
}
