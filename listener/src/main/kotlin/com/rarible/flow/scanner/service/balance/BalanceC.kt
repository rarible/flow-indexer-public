package com.rarible.flow.scanner.service.balance

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.cadence.CadenceNamespace
import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.JsonCadenceConversion
import com.nftco.flow.sdk.cadence.JsonCadenceConverter
import com.rarible.flow.core.domain.Balance
import java.math.BigDecimal

@JsonCadenceConversion(BalanceC.Companion.CadenceConverter::class)
data class BalanceC(
    val account: FlowAddress,
    val token: String,
    val amount: BigDecimal
) {

    fun toDomain(): Balance {
        return Balance(
            account,
            token,
            amount
        )
    }

    companion object {
        class CadenceConverter : JsonCadenceConverter<BalanceC> {
            override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): BalanceC = com.nftco.flow.sdk.cadence.unmarshall(value) {
                BalanceC(
                    FlowAddress(address("account")),
                    string("token"),
                    bigDecimal("amount")
                )
            }
        }
    }
}
