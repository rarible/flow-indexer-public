package com.rarible.flow.api.meta.provider

object HotWheelsCardMetaParser : MattelMetaParser() {

    override fun getName(map: Map<String, String>): String? {
        return map.get("carName") + " #" + map.get("cardId")
    }

    override val fieldName = fields("carName")
    override val fieldDescription = fields()
    override val fieldImageOriginal = fields("imageUrl")
    override val fieldRights = fields("licensorLegal")

    override val attributesWhiteList = setOf(
        "seriesName",
        "releaseYear",
        "rarity",
        "redeemable",
        "type",
        "mint",
        "totalSupply",
        "cardId",
        "miniCollection"
    )

}