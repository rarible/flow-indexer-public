package com.rarible.flow.api.meta.provider

import com.rarible.flow.api.meta.getFirst

object BarbieCardMetaParser : MattelMetaParser() {

    override fun getName(map: Map<String, String>): String? {
        return map.getFirst(*BarbieTokenMetaParser.fieldName) + " #" + map.get("cardId")
    }

    override val fieldName = fields("name")
    override val fieldDescription = fields()
    override val fieldImageOriginal = fields("imageUrl")
    override val fieldRights = fields("licensorLegal")

    override val attributesWhiteList = setOf(
        "type",
    )

}