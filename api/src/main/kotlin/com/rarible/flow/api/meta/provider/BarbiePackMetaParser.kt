package com.rarible.flow.api.meta.provider

import com.rarible.flow.api.meta.getFirst

object BarbiePackMetaParser : MattelMetaParser() {

    override fun getName(map: Map<String, String>): String? {
        return map.getFirst(*fieldName)
    }

    override val fieldName = fields("collectionName")
    override val fieldDescription = fields("packDescription")
    override val fieldImageOriginal = fields("thumbnailCID")
    override val fieldRights = fields()

    override val attributesWhiteList = setOf(
        "type",
        "totalItemCount",
        "packName"
    )
}