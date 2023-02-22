package com.rarible.flow.api.meta.provider

import com.rarible.flow.api.service.UrlService
import com.rarible.flow.core.domain.ItemId
import org.springframework.stereotype.Component

@Component
class HotWheelsCardMetaProvider(
    rawPropertiesProvider: RawMetaProvider,
    urlService: UrlService
) : HotWheelsMetaProvider(
    rawPropertiesProvider,
    urlService
) {

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.endsWith(".HWGarageCard")

    override val fieldName = listOf(
        // TODO add or replace
        "name"
    ).toTypedArray()

    override val fieldDescription = listOf(
        // TODO add or replace
        "description"
    ).toTypedArray()

    override val fieldImageOriginal = listOf(
        // TODO add or replace
        "image"
    ).toTypedArray()

    override val fieldImagePreview = listOf(
        // TODO add or replace
        "imagePreview"
    ).toTypedArray()

    override val fieldImageBig = listOf(
        // TODO add or replace
        "imageBig"
    ).toTypedArray()

    override val fieldVideoOriginal = listOf(
        // TODO add or replace
        "animation"
    ).toTypedArray()

    override val fieldExternalUri = listOf(
        // TODO add or replace
        "external_url"
    ).toTypedArray()

    override val attributesWhiteList = setOf(
        // TODO extend
        "releaseYear"
    )

}