package com.rarible.flow.core.domain

import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId
import java.time.Instant

/**
 * Represents NFT Items' meta information
 * @property itemId         - ID of an NFT item (address:tokenId)
 * @property name           - item's card title
 * @property description    - description
 * @property attributes     - item attributes
 * @property contentUrls    - list of content URL's
 *
 */
@Document
data class ItemMeta(
    @MongoId(FieldType.STRING)
    val itemId: ItemId,
    val name: String,
    val description: String,
    val attributes: List<ItemMetaAttribute>,
    val contentUrls: List<String>,
    val createdAt: Instant? = null,
    val tags: List<String>? = null,
    val genres: List<String>? = null,
    val language: String? = null,
    val rights: String? = null,
    val rightsUrl: String? = null,
    val externalUri: String? = null,
    val content: List<Content>? = null,
    val originalMetaUri: String? = null,
) {
    @Field(targetType = FieldType.BINARY)
    var raw: ByteArray? = null

    @Field(targetType = FieldType.STRING)
    var base64: String? = null

    companion object {
        fun empty(itemId: ItemId): ItemMeta = ItemMeta(
            itemId = itemId,
            name = "Untitled",
            description = "",
            attributes = emptyList(),
            contentUrls = emptyList()
        )
    }

    data class Content(
        val url: String,
        val representation: Representation = Representation.ORIGINAL,
        val type: Type,
        val fileName: String? = null,
        val mimeType: String? = null,
        val size: Int? = null,
        val width: Int? = null,
        val height: Int? = null,
    ) {
        enum class Representation {
            ORIGINAL,
            BIG,
            PREVIEW
        }

        enum class Type {
            IMAGE,
            VIDEO,
            AUDIO,
            MODEL_3D,
            HTML,
            UNKNOWN
        }
    }
}

data class ItemMetaAttribute(
    val key: String,
    val value: String?,
    val type: String? = null,
    val format: String? = null,
)
