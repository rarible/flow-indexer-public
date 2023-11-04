package com.rarible.flow.api.meta.provider

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.api.meta.ItemMetaAttribute
import com.rarible.flow.api.meta.JsonPropertiesParser
import com.rarible.flow.api.meta.getArray
import com.rarible.flow.api.meta.getNested
import com.rarible.flow.api.meta.getText
import com.rarible.flow.api.royalty.provider.Royalty
import com.rarible.flow.core.domain.ItemId

object GamisodesMetaParser {

    private const val SECTION_EDITIONS = "Editions"
    private const val SECTION_DISPLAY = "Display"
    private const val SECTION_EXTERNAL_URL = "ExternalURL"
    private const val SECTION_ROYALTIES = "Royalties"
    private const val SECTION_TRAITS = "Traits"

    private const val VALUE = "value"
    private const val NAME = "name"
    private const val ID = "id"

    private val root = listOf(VALUE, VALUE, VALUE)
    private val nested = listOf(VALUE, VALUE)

    fun parse(json: String, itemId: ItemId): GamisodesMeta {
        val jsonNode = JsonPropertiesParser.parse(itemId, json)
        val rootArray = jsonNode.getArray(VALUE)
        val sections = rootArray.mapNotNull(this::getSectionNode)
            .associateBy({ it.first }, { it.second })

        val royalties = sections[SECTION_ROYALTIES]?.getFieldArray("cutInfos")?.mapNotNull { royaltyNode ->
            val fields = royaltyNode.getFields()
            val address = fields["receiver"]?.getText("address")?.let { FlowAddress(it) } ?: return@mapNotNull null
            val part = fields.getFieldText("cut")?.toBigDecimal() ?: return@mapNotNull null
            Royalty(address = address.formatted, fee = part)
        }

        val traits = sections[SECTION_TRAITS]?.getFieldArray("traits")?.mapNotNull { traitNode ->
            val fields = traitNode.getFields()
            fields.getFieldText(NAME)?.let { key ->
                ItemMetaAttribute(key = key, value = fields.getFieldText(VALUE))
            }
        } ?: emptyList()

        val infoList = sections[SECTION_EDITIONS]?.getFieldArray("infoList")
            ?.find { it.getText(ID)?.endsWith("Edition") == true }
            ?.getFields() ?: emptyMap()

        val extraTraits = listOfNotNull(
            infoList.getFieldText("number")?.let { ItemMetaAttribute("number", it) },
            infoList.getFieldText("max")?.let { ItemMetaAttribute("max", it) }
        )

        return GamisodesMeta(
            name = sections[SECTION_DISPLAY]?.getFieldText("name") ?: "Untitled",
            description = sections[SECTION_DISPLAY]?.getFieldText("description"),
            externalUri = sections[SECTION_EXTERNAL_URL]?.getFieldText("url"),
            imagePreview = sections[SECTION_DISPLAY]?.get("thumbnail")?.getFields()?.getFieldText("url"),
            imageOriginal = traits.find { it.key == "decentralizedMediaFiles" }?.value,
            attributes = traits + extraTraits,
            royalties = royalties ?: emptyList()
        )
    }

    private fun getSectionNode(node: JsonNode): Pair<String, Map<String, JsonNode>>? {
        val sectionNode = node.getNested(root)
        val sectionName = sectionNode.getText(ID)?.substringAfterLast(".") ?: return null
        return sectionName to sectionNode.getFields()
    }

    private fun Map<String, JsonNode>.getFieldText(fieldName: String): String? {
        return this[fieldName]?.asText() ?: return null
    }

    private fun Map<String, JsonNode>.getFieldArray(fieldName: String): Iterable<JsonNode> {
        return (this[fieldName] as? ArrayNode)
            ?.map { it.get(VALUE) }
            ?: emptyList()
    }

    private fun JsonNode.getFields(): Map<String, JsonNode> {
        val nestedFields = this.getArray("fields")
        return nestedFields.associateBy({ it.getText(NAME)!! }, {
            // Optional values are nested in another "value" object
            val valueNode = it.get(VALUE)
            if (valueNode.getText("type") == "Optional") {
                valueNode.getNested(nested)
            } else {
                valueNode.get(VALUE)
            }
        })
    }
}

data class GamisodesMeta(
    val name: String,
    val description: String?,
    val externalUri: String?,
    val imageOriginal: String?,
    val imagePreview: String?,
    val attributes: List<ItemMetaAttribute> = emptyList(),
    val royalties: List<Royalty> = emptyList(),
)