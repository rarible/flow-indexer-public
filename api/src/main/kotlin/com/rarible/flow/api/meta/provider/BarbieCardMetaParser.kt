package com.rarible.flow.api.meta.provider

import com.rarible.flow.api.meta.getFirst
import com.rarible.flow.core.domain.ItemId

// TODO update when know meta format
object BarbieCardMetaParser : MattelMetaParser() {

    override fun getName(map: Map<String, String>, itemId: ItemId): String? {
        return map.getFirst(*BarbieTokenMetaParser.fieldName)
    }

    override val fieldName = fields("name")
    override val fieldDescription = fields()
    override val fieldImageOriginal = fields("imageUrl")
    override val fieldRights = fields("eula")

    override val attributesWhiteList = setOf(
        "Type",
        "Background",
        "Eyes",
        "Rarity",
        "Hair",
        "Nose",
        "releaseYear",
        "cardId",
        "releaseDate",
        "Skin Tone",
        "Lips",
        "Necklace",
        "Lip Color",
        "Face Shape",
        "Hair Color",
        "Freckles",
        "Earrings",
        "seriesName",
        "Eyebrows Color",
        "Eye Color",
        "Eyebrows",
        "miniCollection",
        "totalSupply",
        "Redeemable",
        "Makeup",
        "Glasses"
    )
}