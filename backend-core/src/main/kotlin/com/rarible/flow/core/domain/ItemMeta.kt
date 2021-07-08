package com.rarible.flow.core.domain

import org.springframework.data.mongodb.core.mapping.Document
import java.net.URI

/**
 * Represents NFT Items' meta information
 * itemId - ID of an NFT item (address:tokenId)
 * title - item's card title
 * description
 * uri - URI to the NFT's media resource
 */
@Document
data class ItemMeta(
    val itemId: String,
    val title: String,
    val description: String,
    val uri: URI
)
