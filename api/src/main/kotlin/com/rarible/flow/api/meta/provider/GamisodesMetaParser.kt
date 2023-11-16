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

    private const val VALUE = "value"
    private const val NAME = "name"
    private const val ID = "id"
    private const val TRAITS = "traits"

    private val root = listOf(VALUE, VALUE, VALUE)
    private val nested = listOf(VALUE, VALUE)
    private val attrs = listOf("platform", "mintLevel", "collection", "rank", "type", "property", "editionSize", "series", "artist", "mimeType", "mediaUrl", "posterUrl", "traits")

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
            royalties = royalties ?: emptyList(),
            setId = sections[SECTION_NFT]?.getFieldText("setId"),
            templateId = sections[SECTION_NFT]?.getFieldText("templateId")
        )
    }

    fun parseAttributes(json: String, itemId: ItemId): List<ItemMetaAttribute> {
        val jsonNode = JsonPropertiesParser.parse(itemId, json)
        val dict = jsonNode.getArray("value")
            .associateBy({ it.at("/key").getText("value") }, { it.at("/value").getText("value") })
            .filterKeys { attrs.contains(it) }
        val traits = getTraits(dict[TRAITS])
        return dict.filter { it.key != TRAITS }.mapNotNull { (key, value) ->
            key?.let {
                ItemMetaAttribute(key, value)
            } ?: null
        } + traits
    }

    private fun getTraits(value: String?): List<ItemMetaAttribute> {
        if (null == value) return emptyList()

        return try {
            val list: List<GamisodesMetaTrait> = objectMapper.readValue(value)
            list.map {
                ItemMetaAttribute(it.traitType, it.value)
            }
        } catch (e: Exception) {
            logger.warn("Failed to read traits from: $value", e)
            return emptyList()
        }
    }

    private fun getSectionNode(node: JsonNode): Pair<String, Map<String, JsonNode>>? {
        val section = node.getNested(root)
        val sectionNode = if (section.isEmpty) node.getNested(nested) else section
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
    val setId: String? = null,
    val templateId: String? = null,
)

data class GamisodesMetaTrait(

    @JsonProperty("trait_type")
    val traitType: String,

    val value: String,
)
