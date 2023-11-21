package com.rarible.flow.api.meta.provider

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.api.meta.ItemMetaAttribute
import com.rarible.flow.api.meta.JsonPropertiesParser
import com.rarible.flow.api.meta.getArray
import com.rarible.flow.api.meta.getNested
import com.rarible.flow.api.meta.getText
import com.rarible.flow.api.royalty.provider.Royalty
import com.rarible.flow.core.domain.ItemId
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object GamisodesMetaParser {

    private const val SECTION_EDITIONS = "Editions"
    private const val SECTION_DISPLAY = "Display"
    private const val SECTION_EXTERNAL_URL = "ExternalURL"
    private const val SECTION_ROYALTIES = "Royalties"
    private const val SECTION_TRAITS = "Traits"
    private const val SECTION_NFT = "NFT"
    private const val SECTION_SERIAL = "Serial"

    private const val VALUE = "value"
    private const val NAME = "name"
    private const val ID = "id"
    private const val TRAITS = "traits"

    private val root = listOf(VALUE, VALUE)
    private val nested = listOf(VALUE, VALUE)
    private val attrs = setOf(
        "platform",
        "collection",
        "mintLevel",
        "mediaType",
        "rank",
        "type",
        "property",
        "editionSize",
        "series",
        "artist",
        "mimeType",
        "mediaUrl",
        "posterUrl",
        TRAITS,
    )

    private val objectMapper = jacksonObjectMapper()
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

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

        val traits = sections[SECTION_TRAITS]?.let {
            getLegacyAttributes(it) + getAttributes(it)
        } ?: emptyList()

        val infoList = sections[SECTION_EDITIONS]?.getFieldArray("infoList")
            ?.find { it.getText(ID)?.endsWith("Edition") == true }
            ?.getFields() ?: emptyMap()

        val extraTraits = listOfNotNull(
            infoList.getFieldText("number")?.let { ItemMetaAttribute("number", it) },
            infoList.getFieldText("max")?.let { ItemMetaAttribute("max", it) },
            sections[SECTION_SERIAL]?.get("number")?.asText()?.let { ItemMetaAttribute("serialNumber", it) }
        )

        return GamisodesMeta(
            name = sections[SECTION_DISPLAY]?.getFieldText("name") ?: "Untitled",
            description = sections[SECTION_DISPLAY]?.getFieldText("description"),
            externalUri = sections[SECTION_EXTERNAL_URL]?.getFieldText("url"),
            imagePreview = sections[SECTION_DISPLAY]?.get("thumbnail")?.getFields()?.getFieldText("url"),
            imageOriginal = traits.find { it.key == "decentralizedMediaFiles" }?.value,
            attributes = traits + extraTraits,
            royalties = royalties ?: emptyList(),
            setId = sections[SECTION_NFT]?.getFieldText("setId"),
            templateId = sections[SECTION_NFT]?.getFieldText("templateId")
        )
    }

    private fun getLegacyAttributes(section: Map<String, JsonNode>): List<ItemMetaAttribute> {
        return section.getFieldArray("traits")?.mapNotNull { traitNode ->
            val fields = traitNode.getFields()
            fields.getFieldText(NAME)?.let { key ->
                ItemMetaAttribute(key = key, value = fields.getFieldText(VALUE))
            }
        }
    }

    private fun getAttributes(section: Map<String, JsonNode>): List<ItemMetaAttribute> {
        return section.mapNotNull { it.key to it.value.getText("value") }
            ?.filter { it.first in attrs }
            ?.map { (key, value) ->
                when (key) {
                    TRAITS -> getNestedTraits(value).map { it.traitType to it.value }
                    else -> listOf(key to value)
                }.toList()
            }?.flatten()
            ?.filterNot { it.second?.isNullOrBlank() == true }
            ?.map { (key, value) ->
                ItemMetaAttribute(key = key, value = value)
            }
    }

    private fun getNestedTraits(value: String?): List<GamisodesMetaTrait> {
        if (null == value) return emptyList()
        return try {
            val list: List<GamisodesMetaTrait> = objectMapper.readValue(value)
            list
        } catch (e: Exception) {
            logger.warn("Failed to read traits from: $value", e)
            return emptyList()
        }
    }

    private fun getSectionNode(node: JsonNode): Pair<String, Map<String, JsonNode>>? {
        val sectionNode = node.getNested(root)
        val sectionName = node.getNested(listOf("key", "value")).textValue().split(".")?.last() ?: return null
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
        return if (this is ArrayNode) {
            this.associateBy({ it.getText(listOf("key", "value"))!! }, { it.get("value") })
        } else {
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
}

data class GamisodesMeta(
    val name: String,
    val description: String?,
    val externalUri: String?,
    val imageOriginal: String?,
    val imagePreview: String?,
    val attributes: List<ItemMetaAttribute> = emptyList(),
    val royalties: List<Royalty> = emptyList(),
    val setId: String? = null,
    val templateId: String? = null,
)

data class GamisodesMetaTrait(

    @JsonProperty("trait_type")
    val traitType: String,

    val value: String,
)
