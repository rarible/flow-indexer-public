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
            .find { it.getText("name") == "metadata" }
            ?.get("value")
            ?: throw MetaException("'metadata' node not found", MetaException.Status.CORRUPTED_DATA)

        return super.map(itemId, dictionary)
    }

    // "seriesName" - for v1, "carName" - for v2
    override val fieldName = fields("seriesName", "carName")
    override val fieldDescription = fields()
    override val fieldImageOriginal = fields("thumbnailCID")
    override val fieldRights = fields()

    override val attributesWhiteList = setOf(
        // for v1
        "totalItemCount",
        // for v2
        "tokenReleaseDate",
        "tokenExpireDate"
    )
}