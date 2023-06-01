package com.rarible.flow.api.meta.provider

import com.fasterxml.jackson.databind.JsonNode
import com.rarible.flow.api.meta.ItemMeta
import com.rarible.flow.api.meta.ItemMetaAttribute
import com.rarible.flow.api.meta.ItemMetaContent
import com.rarible.flow.api.meta.JsonPropertiesParser
import com.rarible.flow.api.meta.MetaException
import com.rarible.flow.api.meta.getArray
import com.rarible.flow.api.meta.getFirst
import com.rarible.flow.api.meta.getText
import com.rarible.flow.core.domain.ItemId
import org.slf4j.LoggerFactory

abstract class MattelMetaParser {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun parse(json: String, itemId: ItemId): ItemMeta {
        val jsonNode = JsonPropertiesParser.parse(itemId, json)
        logger.info("Received json for item {}: {}", itemId, json)
        return map(itemId, jsonNode)
    }

    abstract fun getName(map: Map<String, String>, itemId: ItemId): String?

    protected open fun map(itemId: ItemId, node: JsonNode): ItemMeta {
        val dictionary = node.get("value")
            .getArray("fields")
            .find { it.getText("name") == "metadata" }
            ?.get("value")
            ?: throw MetaException("'metadata' node not found", MetaException.Status.CORRUPTED_DATA)
        val map = toKeyValueMap(dictionary)
        val name = getName(map, itemId)
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
            attributes = map.map {
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

    protected fun fields(vararg fields: String): Array<String> {
        return fields.toList().toTypedArray()
    }

}