package com.rarible.flow.core.domain

import com.nftco.flow.sdk.FlowAddress
import java.io.Serializable

data class OwnershipId(
    val contract: String,
    val tokenId: TokenId,
    val owner: FlowAddress
) : Serializable {

    override fun toString(): String {
        return "${contract}:$tokenId:${owner.formatted}"
    }

    companion object {

        fun parse(str: String): OwnershipId {
            val parts = str.split(':')
            if (parts.size == 3) {
                return OwnershipId(
                    contract = parts[0],
                    tokenId = parts[1].toLong(),
                    owner = FlowAddress(parts[2])
                )
            } else {
                throw IllegalArgumentException("Failed to parse OwnershipId from [$str]")
            }
        }
    }
}