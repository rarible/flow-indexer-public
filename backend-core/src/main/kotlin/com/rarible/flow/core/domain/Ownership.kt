package com.rarible.flow.core.domain

import org.onflow.sdk.FlowAddress
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.MongoId
import java.time.Instant


data class OwnershipId(
    val contract: FlowAddress,
    val tokenId: TokenId,
    val owner: FlowAddress
) {

    override fun toString(): String {
        return "${contract.formatted}:$tokenId:${owner.formatted}"
    }

    companion object {
        fun parse(str: String): OwnershipId {
            val parts = str.split(':')
            if(parts.size == 3) {
                return OwnershipId(
                    FlowAddress(parts[0]),
                    parts[1].toLong(),
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
    val tokenId: TokenId,
    val owner: FlowAddress,
    val date: Instant
) {
    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: OwnershipId
        get() = OwnershipId(contract, tokenId, owner)
        set(_) {}
}
