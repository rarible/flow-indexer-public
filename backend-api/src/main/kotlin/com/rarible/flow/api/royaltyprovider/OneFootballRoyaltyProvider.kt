package com.rarible.flow.api.royaltyprovider

import com.rarible.flow.Contracts
import com.rarible.flow.api.config.ApiProperties
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class OneFootballRoyaltyProvider(
    private val apiProperties: ApiProperties
) : ItemRoyaltyProvider {

    override fun isSupported(itemId: ItemId): Boolean =
        itemId.contract.contains(Contracts.ONE_FOOTBALL.contractName)

    override suspend fun getRoyalty(item: Item): List<Royalty> {
        val royaltyAddress = Contracts.ONE_FOOTBALL.deployments[apiProperties.chainId]
        return if (royaltyAddress == null) emptyList()
        else listOf(
            Royalty(
                royaltyAddress.formatted,
                FEE
            )
        )
    }

    companion object {
        val FEE = BigDecimal("0.005") // 0.5%
    }
}
