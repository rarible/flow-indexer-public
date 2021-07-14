package com.rarible.flow.core.domain

import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.mapping.Unwrapped
import java.lang.IllegalArgumentException
import java.time.Instant



data class OwnershipId(
    val contract: Address,
    val tokenId: Long,
    val owner: Address
) {

    override fun toString(): String {
        return "$contract:$tokenId:$owner"
    }

    companion object {
        fun of(str: String): OwnershipId {
            val parts = str.split(':')
            if(parts.size == 3) {
                return OwnershipId(
                    Address(parts[0]),
                    parts[1].toLong(),
                    Address(parts[2])
                )
            } else {
                throw IllegalArgumentException("Failed to parse OwnershipId from $str")
            }
        }
    }
}

data class Ownership(
    val contract: Address,
    val tokenId: Long,
    val owner: Address,
    val date: Instant
) {
    private val _id: String = OwnershipId(contract, tokenId, owner).toString()

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: String
        get() = _id
        set(_) {}
}
