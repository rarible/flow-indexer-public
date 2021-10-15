package com.rarible.flow.core.domain

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.net.URI

/**
 * Represents NFT Items' meta information
 * @property itemId         - ID of an NFT item (address:tokenId)
 * @property title          - item's card title
 * @property description    - description
 * @property uri            - URI to the NFT's media resource
 * @property properties     - additional properties
 *
 */
@Document
data class ItemMeta(
    @Id
    val itemId: ItemId,
    val title: String,
    val description: String,
    val uri: URI,
    val properties: Map<String, Any> = emptyMap()
)
