package com.rarible.flow.api.meta.provider

import com.rarible.flow.api.meta.getFirst
import com.rarible.flow.core.domain.ItemId

object BarbieCardMetaParser : MattelMetaParser() {

    override fun getName(map: Map<String, String>, itemId: ItemId): String? {
        return map.getFirst(*BarbieTokenMetaParser.fieldName) + "#" + map.getFirst("cardId")
    }

    override val fieldName = fields("name")
    override val fieldDescription = fields()
    override val fieldImageOriginal = fields("imageUrl")
    override val fieldRights = fields("eula")

    override val attributesWhiteList = setOf(
        "lips",
        "hairColor",
        "mint",
        "hair",
        "tokenId",
        "nose",
        "eyeColor",
        "makeup",
        "releaseYear",
        "eyebrowsColor",
        "freckles",
        "cardId",
        "glasses",
        "releaseDate",
        "career",
        "eyes",
        "version",
        "skinTone",
        "lipColor",
        "seriesName",
        "earrings",
        "faceShape",
        "type",
        "rarity",
        "firstAppearance",
        "necklace",
        "background",
        "eyebrows",
        "miniCollection",
        "totalSupply",
        "editionSize",
        "redeemable"
    )
}