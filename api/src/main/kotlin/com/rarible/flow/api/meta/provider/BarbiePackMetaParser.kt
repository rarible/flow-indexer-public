package com.rarible.flow.api.meta.provider

import com.rarible.flow.api.meta.getFirst
import com.rarible.flow.core.domain.ItemId

object BarbiePackMetaParser : MattelMetaParser() {

    override fun getName(map: Map<String, String>, itemId: ItemId): String? {
        return map.getFirst(*fieldName) + " #" + itemId.tokenId
    }

    override val fieldName = fields("packName")
    override val fieldDescription = fields("packDescription")
    override val fieldImageOriginal = fields("thumbnailCID")
    override val fieldRights = fields()

    override val attributesWhiteList = setOf(
        "type",
        "totalItemCount",
        "collectionName"
    )
}