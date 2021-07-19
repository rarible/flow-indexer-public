package com.rarible.flow.core.domain

import org.onflow.sdk.FlowAddress
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.mapping.Unwrapped
import java.lang.IllegalArgumentException
import java.math.BigInteger
import java.time.Instant



data class OwnershipId(
    val contract: FlowAddress,
    val tokenId: BigInteger,
    val owner: FlowAddress
) {

    override fun toString(): String {
        return "$contract:$tokenId:$owner"
    }

    companion object {
        fun parse(str: String): OwnershipId {
            val parts = str.split(':')
            if(parts.size == 3) {
                return OwnershipId(
                    FlowAddress(parts[0]),
                    BigInteger(parts[1]),
                    FlowAddress(parts[2])
                )
            } else {
                throw IllegalArgumentException("Failed to parse OwnershipId from [$str]")
            }
        }
    }
}

data class Ownership(
    val contract: FlowAddress,
    val tokenId: BigInteger,
    val owner: FlowAddress,
    val date: Instant
) {
    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: OwnershipId
        get() = OwnershipId(contract, tokenId, owner)
        set(_) {}
}
