package com.rarible.flow.api.meta.provider

import com.rarible.flow.api.meta.getFirst

object BarbieTokenMetaParser : MattelMetaParser() {

    override fun getName(map: Map<String, String>): String? {
        return map.getFirst(*fieldName) + " #" + map.get("cardId")
    }

    override val fieldName = fields("name")
    override val fieldDescription = fields()
    override val fieldImageOriginal = fields("imageUrl")
    override val fieldRights = fields("licensorLegal")

    override val attributesWhiteList = setOf(
        "type",
    )

}