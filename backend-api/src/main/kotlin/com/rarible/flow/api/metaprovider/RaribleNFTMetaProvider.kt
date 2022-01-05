package com.rarible.flow.api.metaprovider

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import com.rarible.flow.log.Log
import org.springframework.boot.json.JacksonJsonParser
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull

@Component
class RaribleNFTMetaProvider(
    //TODO private val pinataClient: WebClient
) : ItemMetaProvider {

    private val logger by Log()

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.contains("RaribleNFT")

    override suspend fun getMeta(item: Item): ItemMeta? {
        var url = item.meta ?: return null
        if (url.startsWith("{")) {
            url = JacksonJsonParser().parseMap(url)["metaURI"] as String?
                ?: return null
        }


        if (url.startsWith("ipfs://")) {
            url = url.substring("ipfs:/".length)
        }

        if (url.isEmpty()) {
            return null
        }

        //TODO inject pinataClient
        val client = WebClient.create("https://rarible.mypinata.cloud/")
        return try {
            val data = client.get().uri(url)
                .retrieve().awaitBodyOrNull<RaribleNFTMetaBody>() ?: return null
            ItemMeta(
                itemId = item.id,
                name = data.name,
                description = data.description,
                attributes = data.attributes.map {
                    ItemMetaAttribute(
                        key = it.key ?: it.traitType!!,
                        value = it.value
                    )
                },
                contentUrls = listOfNotNull(data.image, data.animationUrl),
            ).apply {
                raw = data.toString().toByteArray(charset = Charsets.UTF_8)
            }
        } catch (e: Exception) {
            logger.warn("Failed RaribleNFTMetaProvider.getMeta({})", item, e)
            return null
        }
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
)

data class RaribleNftAttr(
    val key: String?,
    @get:JsonProperty("trait_type")
    val traitType: String?,
    val value: String?
)
