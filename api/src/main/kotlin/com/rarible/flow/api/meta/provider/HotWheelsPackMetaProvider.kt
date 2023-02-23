package com.rarible.flow.api.meta.provider

import com.fasterxml.jackson.databind.JsonNode
import com.rarible.flow.api.meta.ItemMeta
import com.rarible.flow.api.meta.MetaException
import com.rarible.flow.api.meta.getArray
import com.rarible.flow.api.meta.getText
import com.rarible.flow.core.domain.ItemId
import org.springframework.stereotype.Component

@Component
class HotWheelsPackMetaProvider : HotWheelsMetaProvider() {

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
        "seriesName"
    ).toTypedArray()

    override val fieldDescription = listOf<String>(
        // TODO add or replace or remove
    ).toTypedArray()

    override val fieldImageOriginal = listOf<String>(
        // TODO add or replace
        "thumbnailCID"
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
        // TODO add or replace or remove
    ).toTypedArray()

    override val attributesWhiteList = setOf(
        // TODO extend
        "totalItemCount"
    )

}