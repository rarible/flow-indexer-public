package com.rarible.flow.api.meta.provider

import com.rarible.flow.core.domain.ItemId
import org.springframework.stereotype.Component

@Component
class HotWheelsCardMetaProvider : HotWheelsMetaProvider() {

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.endsWith(".HWGarageCard")

    override val fieldName = listOf(
        // TODO add or replace
        "carName"
    ).toTypedArray()

    override val fieldDescription = listOf<String>(
        // TODO add or replace
    ).toTypedArray()

    override val fieldImageOriginal = listOf(
        // TODO add or replace
        "imageUrl"
    ).toTypedArray()

    override val fieldImagePreview = listOf<String>(
        // TODO add or replace or remove
    ).toTypedArray()

    override val fieldImageBig = listOf<String>(
        // TODO add or replace or remove
    ).toTypedArray()

    override val fieldVideoOriginal = listOf<String>(
        // TODO add or replace or remove
    ).toTypedArray()

    override val fieldExternalUri = listOf<String>(
        // TODO add or replace or remove
    ).toTypedArray()

    override val fieldRights = listOf<String>(
        "licensorLegal"
    ).toTypedArray()

    override val attributesWhiteList = setOf(
        // TODO extend
        "seriesName",
        "releaseYear",
        "rarity",
        "redeemable",
        "type"
    )

}