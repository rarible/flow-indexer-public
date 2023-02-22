package com.rarible.flow.api.meta.provider

import com.fasterxml.jackson.databind.JsonNode
import com.rarible.flow.api.meta.ItemMeta
import com.rarible.flow.api.meta.MetaException
import com.rarible.flow.api.meta.getArray
import com.rarible.flow.api.meta.getText
import com.rarible.flow.api.service.UrlService
import com.rarible.flow.core.domain.ItemId
import org.springframework.stereotype.Component

@Component
class HotWheelsPackMetaProvider(
    rawPropertiesProvider: RawMetaProvider,
    urlService: UrlService
) : HotWheelsMetaProvider(
    rawPropertiesProvider,
    urlService
) {

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.endsWith(".HWGaragePack")

    override fun map(itemId: ItemId, node: JsonNode): ItemMeta {
        val dictionary = node.get("value")
            .getArray("fields")
            .find { it.getText("name") == "Dictionary" }
            ?: throw MetaException("'Dictionary' node not found", MetaException.Status.CORRUPTED_DATA)

        return super.map(itemId, dictionary)
    }

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