package com.rarible.flow.api.meta.provider

import com.fasterxml.jackson.databind.JsonNode
import com.rarible.core.meta.resource.model.UrlResource
import com.rarible.flow.api.meta.ItemMeta
import com.rarible.flow.api.meta.ItemMetaAttribute
import com.rarible.flow.api.meta.ItemMetaContent
import com.rarible.flow.api.meta.JsonPropertiesParser
import com.rarible.flow.api.meta.MetaException
import com.rarible.flow.api.meta.fetcher.RawMetaFetcher
import com.rarible.flow.api.meta.getArray
import com.rarible.flow.api.meta.getText
import com.rarible.flow.api.service.UrlService
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import org.springframework.stereotype.Component

@Component
class RaribleMetaProvider(
    private val rawPropertiesProvider: RawMetaFetcher,
    private val urlService: UrlService
) : ItemMetaProvider {

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.endsWith(".RaribleNFT")

    override suspend fun getMeta(item: Item): ItemMeta? {
        val resource = readUrl(item)
        val json = rawPropertiesProvider.getContent(item.id, resource) ?: throw MetaException(
            "Failed to get meta",
            MetaException.Status.ERROR
        )

        val jsonNode = JsonPropertiesParser.parse(item.id, json)
        return map(item.id, jsonNode)
    }

    private fun readUrl(item: Item): UrlResource {
        val itemId = item.id.toString()
        val rawUrl = item.meta
            ?: throw MetaException("Item $itemId doesn't have metaURI", MetaException.Status.NOT_FOUND)

        urlService.parseUrl(rawUrl, itemId)?.let { return it }

        val json = JsonPropertiesParser.parse(itemId, rawUrl)
        val url = json.get("metaURI").asText()

        urlService.parseUrl(url, itemId)?.let { return it }

        throw MetaException("Unparseable metaURI in json: $url", MetaException.Status.CORRUPTED_URL)
    }

    private fun map(itemId: ItemId, node: JsonNode): ItemMeta {
        val name = node.getText("name")
            ?: throw MetaException("'name' field not found", MetaException.Status.CORRUPTED_DATA)

        return ItemMeta(
            itemId = itemId,
            name = name,
            description = node.getText("description") ?: "",
            attributes = node.getArray("attributes").mapNotNull {
                val key = it.getText("key", "trait_type")
                val value = it.getText("value")
                key?.let { ItemMetaAttribute(key, value) }
            },
            content = listOfNotNull(
                node.getText("image")?.let { ItemMetaContent(it, ItemMetaContent.Type.IMAGE) },
                node.getText("animation_url")?.let { ItemMetaContent(it, ItemMetaContent.Type.VIDEO) }
            )
        )
    }
}