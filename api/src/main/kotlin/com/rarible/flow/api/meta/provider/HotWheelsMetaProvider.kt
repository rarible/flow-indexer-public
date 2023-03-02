package com.rarible.flow.api.meta.provider

import com.fasterxml.jackson.databind.JsonNode
import com.rarible.flow.api.meta.ItemMeta
import com.rarible.flow.api.meta.ItemMetaAttribute
import com.rarible.flow.api.meta.ItemMetaContent
import com.rarible.flow.api.meta.JsonPropertiesParser
import com.rarible.flow.api.meta.MetaException
import com.rarible.flow.api.meta.fetcher.HWMetaFetcher
import com.rarible.flow.api.meta.getArray
import com.rarible.flow.api.meta.getFirst
import com.rarible.flow.api.meta.getText
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId

abstract class HotWheelsMetaProvider(
    private val fetcher: HWMetaFetcher
) : ItemMetaProvider {

    override suspend fun getMeta(item: Item): ItemMeta? {
        val json = fetcher.getContent(item.id)
            ?: throw MetaException("Item ${item.id} doesn't have meta json", MetaException.Status.NOT_FOUND)

        val jsonNode = JsonPropertiesParser.parse(item.id, json)
        return map(item.id, jsonNode)
    }

    abstract fun getName(map: Map<String,String>) : String?
    protected open fun map(itemId: ItemId, node: JsonNode): ItemMeta {
        val dictionary = node.get("value")
            .getArray("fields")
            .find { it.getText("name") == "metadata" }
            ?.get("value")
            ?: throw MetaException("'metadata' node not found", MetaException.Status.CORRUPTED_DATA)
        val map = toKeyValueMap(dictionary)
        val name = getName(map)
            ?: throw MetaException("'name' field not found", MetaException.Status.CORRUPTED_DATA)
        return ItemMeta(
            itemId = itemId,
            name = name,
            description = map.getFirst(*fieldDescription) ?: "",
            rights = map.getFirst(*fieldRights),
            content = listOfNotNull(
                map.getFirst(*fieldImageOriginal)?.let {
                    ItemMetaContent(it, ItemMetaContent.Type.IMAGE)
                }
            ),
            attributes = map.filter { attributesWhiteList.contains(it.key) }.map {
                ItemMetaAttribute(
                    key = it.key,
                    value = it.value
                )
            }
        )
    }

    /**
     *  Example:
     *  "value": [
     *    {
     *      "key":   {"type": "String", "value": "packHash"},
     *      "value": {"type": "String", "value": "123"}
     *    }
     *  ]
     */
    private fun toKeyValueMap(node: JsonNode): Map<String, String> {
        val values = node.getArray("value").ifEmpty {
            throw MetaException("root 'value' field not found", MetaException.Status.CORRUPTED_DATA)
        }

        return values.mapNotNull { attrNode ->
            // TODO filter if type != String ?
            val key = attrNode.getText(listOf("key", "value")) ?: return@mapNotNull null
            val value = attrNode.getText(listOf("value", "value")) ?: return@mapNotNull null
            // TODO pretty traits? seriesName -> Series name
            key to value
        }.toMap()
    }

    abstract val fieldName: Array<String>
    abstract val fieldDescription: Array<String>
    abstract val fieldImageOriginal: Array<String>
    abstract val fieldRights: Array<String>
    abstract val attributesWhiteList: Set<String>

    protected fun fields(vararg fields: String): Array<String> {
        return fields.toList().toTypedArray()
    }

}