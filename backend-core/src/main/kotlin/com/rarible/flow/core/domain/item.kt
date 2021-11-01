package com.rarible.flow.core.domain

import com.nftco.flow.sdk.FlowAddress
import com.querydsl.core.annotations.QueryEntity
import com.rarible.flow.core.repository.Cont
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId
import java.io.Serializable
import java.time.Instant
import kotlin.reflect.KProperty1

data class Part(
    val address: FlowAddress,
    val fee: Double
)

data class ItemId(val contract: String, val tokenId: TokenId): Serializable {
    override fun toString(): String {
        return "${contract}:$tokenId"
    }

    companion object {
        fun parse(source: String): ItemId {
            val parts = source.split(':')
            if(parts.size == 2) {
                val contract =parts[0]
                val tokenId = parts[1].toLong()
                return ItemId(contract, tokenId)
            } else throw IllegalArgumentException("Failed to parse ItemId from [$source]")
        }
    }
}

typealias TokenId = Long

@QueryEntity
@Document
@CompoundIndexes(
    CompoundIndex(
        name = "date_tokenId",
        def = "{'date': 1, 'tokenId': 1}"
    )
)
data class Item (
    @Indexed
    val contract: String,
    val tokenId: TokenId,
    @Indexed
    val creator: FlowAddress,
    val royalties: List<Part>,
    @Indexed
    val owner: FlowAddress?,
    val mintedAt: Instant,
    val meta: String? = null,
    val listed: Boolean = false,
    @Indexed
    val collection: String,
    @Field(targetType = FieldType.DATE_TIME)
    val updatedAt: Instant
) {

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: ItemId
        get() = ItemId(this.contract, this.tokenId)
        set(_) {}

    fun ownershipId(owner: FlowAddress): OwnershipId {
        return OwnershipId(this.contract, this.tokenId, owner)
    }
}

@Document
data class ItemCollection(
    @MongoId
    val id: String,
    val owner: FlowAddress,
    val name: String,
    val symbol: String,
    val createdDate: Instant = Instant.now()
)

