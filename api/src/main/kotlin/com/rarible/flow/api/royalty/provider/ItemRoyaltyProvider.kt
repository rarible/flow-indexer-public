package com.rarible.flow.api.royalty.provider

import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Part
import java.math.BigDecimal

// todo refactor com.rarible.flow.core.domain.Part::fee Double -> BigDecimal, then replace
data class Royalty(val address: String, val fee: BigDecimal) {
    constructor(part: Part): this(
        address = part.address.formatted,
        fee = part.fee.toBigDecimal()
    )
}

interface ItemRoyaltyProvider {
    fun isSupported(itemId: ItemId): Boolean
    suspend fun getRoyalties(item: Item): List<Royalty>
}
