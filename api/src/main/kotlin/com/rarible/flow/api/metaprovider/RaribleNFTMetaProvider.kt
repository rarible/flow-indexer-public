package com.rarible.flow.api.metaprovider

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rarible.core.meta.resource.model.UrlResource
import com.rarible.flow.api.service.UrlService
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import org.springframework.stereotype.Component

@Component
class RaribleNFTMetaProvider(
    private val rawPropertiesProvider: RawPropertiesProvider,
    private val urlService: UrlService
) : ItemMetaProvider {

    private val mapper = jacksonObjectMapper()

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.endsWith(".RaribleNFT")

    override suspend fun getMeta(item: Item): ItemMeta? {
        val resource = readUrl(item)
        val json = rawPropertiesProvider.getContent(item.id, resource) ?: throw MetaException(
            "Failed to get meta",
            MetaException.Status.ERROR
        )

        // TODO ideally make it manually
        return mapper.readValue(json, RaribleNFTMetaBody::class.java).toItemMeta(item.id)

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
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class RaribleNFTMetaBody(
    val name: String,
    val description: String,
    val image: String? = null,
    @get:JsonProperty("animation_url")
    val animationUrl: String? = null,
    val attributes: List<RaribleNftAttr>
) : MetaBody {

    override fun toItemMeta(itemId: ItemId): ItemMeta {
        return ItemMeta(
            itemId = itemId,
            name = name,
            description = description,
            attributes = attributes.map {
                ItemMetaAttribute(
                    key = it.key ?: it.traitType!!,
                    value = it.value
                )
            },
            contentUrls = emptyList(),
            content = listOfNotNull(
                image?.let {
                    ItemMeta.Content(
                        it,
                        ItemMeta.Content.Representation.ORIGINAL,
                        ItemMeta.Content.Type.IMAGE
                    )
                },
                animationUrl?.let {
                    ItemMeta.Content(
                        it,
                        ItemMeta.Content.Representation.ORIGINAL,
                        ItemMeta.Content.Type.VIDEO
                    )
                }
            ),
        ).apply {
            raw = toString().toByteArray(charset = Charsets.UTF_8)
        }
    }
}

data class RaribleNftAttr(
    val key: String?,
    @get:JsonProperty("trait_type")
    val traitType: String?,
    val value: String?
)
