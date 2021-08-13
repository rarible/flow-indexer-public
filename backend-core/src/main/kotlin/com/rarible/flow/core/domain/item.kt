package com.rarible.flow.core.domain

import org.onflow.sdk.FlowAddress
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId
import java.io.Serializable
import java.math.BigInteger
import java.time.Instant

data class Part(
    val address: FlowAddress,
    val fee: Double
)

data class ItemId(val contract: FlowAddress, val tokenId: TokenId): Serializable {
    override fun toString(): String {
        return "${contract.formatted}:$tokenId"
    }

    companion object {
        fun parse(source: String): ItemId {
            val parts = source.split(':')
            if(parts.size == 2) {
                val contract = FlowAddress(parts[0])
                val tokenId = parts[1].toLong()
                return ItemId(contract, tokenId)
            } else throw IllegalArgumentException("Failed to parse ItemId from [$source]")
        }
    }
}

typealias TokenId = Long

@Document
data class Item(
    val contract: FlowAddress,
    val tokenId: TokenId,
    val creator: FlowAddress,
    val royalties: List<Part>,
    val owner: FlowAddress?,
    val date: Instant,
    val meta: String? = null,
    val listed: Boolean = false,
    val collection: String
) {

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: ItemId
        get() = ItemId(this.contract, this.tokenId)
        set(_) {}


    fun unlist(): Item {
        return copy(listed = false)
    }

    /**
     * Transfer implies de-listing
     */
    fun transfer(to: FlowAddress): Item {
        return unlist().copy(owner = to)
    }
}

@Document
data class ItemCollection(
    @MongoId
    val id: String,
    val owner: FlowAddress,
    val name: String,
    val symbol: String
)

