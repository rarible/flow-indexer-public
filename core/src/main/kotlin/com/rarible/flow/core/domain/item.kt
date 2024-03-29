package com.rarible.flow.core.domain

import com.nftco.flow.sdk.FlowAddress
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId
import java.time.Instant

typealias TokenId = Long

@Document
data class Item(
    val contract: String,
    val tokenId: TokenId,
    val creator: FlowAddress,
    val royalties: List<Part>,
    val owner: FlowAddress?,
    val mintedAt: Instant,
    val meta: String? = null,
    val collection: String,
    @Field(targetType = FieldType.DATE_TIME)
    val updatedAt: Instant,
    @Version
    val version: Long? = null
) {

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: ItemId
        get() = ItemId(this.contract, this.tokenId)
        set(_) {}

    fun ownershipId(owner: FlowAddress): OwnershipId {
        return OwnershipId(this.contract, this.tokenId, owner)
    }

    companion object {

        const val COLLECTION = "item"
    }
}

data class Part(
    val address: FlowAddress,
    val fee: Double
)

@Document
data class ItemCollection(
    @MongoId
    val id: String,
    @Indexed
    val owner: FlowAddress,
    @Indexed
    val name: String,
    @Indexed
    val symbol: String,
    @Indexed
    val createdDate: Instant = Instant.now(),
    val features: Set<String> = emptySet(),
    val isSoft: Boolean = false,
    @Indexed
    val chainId: Long? = null,
    val chainParentId: Long? = null,
    val royalties: List<Part>? = null,
    val description: String? = null,
    val burned: Boolean = false,
    val icon: String? = null,
    val url: String? = null,
    val enabled: Boolean = true
)
