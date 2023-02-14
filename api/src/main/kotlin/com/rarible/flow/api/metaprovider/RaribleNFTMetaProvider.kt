package com.rarible.flow.api.metaprovider

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import org.slf4j.LoggerFactory
import org.springframework.boot.json.JacksonJsonParser
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull

@Component
class RaribleNFTMetaProvider(
    private val ipfsClient: WebClient
) : ItemMetaProvider {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.endsWith(".RaribleNFT")

    override suspend fun getMeta(item: Item): ItemMeta? {
        val ipfs = readUrl(item) ?: return null

        return try {
            return ipfsClient
                .get()
                .uri("/$ipfs")
                .retrieve()
                .awaitBodyOrNull<RaribleNFTMetaBody>()
                ?.toItemMeta(item.id)
        } catch (e: Exception) {
            logger.warn("Failed RaribleNFTMetaProvider.getMeta({})", item, e)
            null
        }
    }

    private fun readUrl(item: Item): String? {
        var url = item.meta ?: return null
        if (url.startsWith("{")) {
            url = JacksonJsonParser().parseMap(url)["metaURI"] as String?
                ?: return null
        }

        if (url.startsWith("ipfs://ipfs/")) {
            url = url.substring("ipfs://ipfs/".length)
        }

        if (url.isEmpty()) {
            return null
        }

        return url
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
): MetaBody {
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
            contentUrls = listOfNotNull(image, animationUrl),
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
