package com.rarible.flow.core.domain

import com.nftco.flow.sdk.FlowAddress
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType
import java.math.BigDecimal
import java.time.Instant

data class BalanceId(
    val account: FlowAddress,
    val token: String
) {

    override fun toString(): String {
        return "$token:${account.formatted}"
    }

    companion object {
        fun of(str: String): BalanceId {
            val (token, acc) = str.split(":")
            return BalanceId(FlowAddress(acc), token)
        }
    }
}

data class Balance(
    val account: FlowAddress,
    val token: String,
    @Field(targetType = FieldType.DECIMAL128)
    val balance: BigDecimal = BigDecimal.ZERO,

    @Field(targetType = FieldType.DATE_TIME)
    val lastUpdatedAt: Instant = Instant.now(),
    @Version
    val version: Long? = null
) {

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: BalanceId
        get() = BalanceId(account, token)
        set(value) {}

    fun withdraw(amount: BigDecimal): Balance {
        return this.copy(balance = this.balance.minus(amount))
    }

    fun deposit(amount: BigDecimal): Balance {
        return this.copy(balance = this.balance.plus(amount))
    }
}