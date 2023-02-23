package com.rarible.flow.api.meta

import com.rarible.flow.core.domain.ItemId
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
data class ItemMeta(
    val itemId: ItemId,
    val name: String,
    val description: String,
    val attributes: List<ItemMetaAttribute>,
    val contentUrls: List<String> = emptyList(),
    val createdAt: Instant? = null,
    val tags: List<String>? = null,
    val genres: List<String>? = null,
    val language: String? = null,
    val rights: String? = null,
    val rightsUrl: String? = null,
    val externalUri: String? = null,
    val content: List<ItemMetaContent> = emptyList(),
    val originalMetaUri: String? = null,
) {

    var raw: ByteArray? = null
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

}

data class ItemMetaAttribute(
    val key: String,
    val value: String?,
    val type: String? = null,
    val format: String? = null,
)
