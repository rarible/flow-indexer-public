package com.rarible.flow.api.metaprovider

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.log.Log
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.boot.json.JacksonJsonParser
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull

@Component
class RaribleNFTMetaProvider(
    private val itemRepository: ItemRepository
) : ItemMetaProvider {

    private val logger by Log()

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.contains("RaribleNFT")

    override suspend fun getMeta(itemId: ItemId): ItemMeta {
        val item = itemRepository.findById(itemId).awaitSingleOrNull()
            ?: return emptyMeta(itemId)
        var url = item.meta ?: return emptyMeta(itemId)
        if (url.startsWith("{")) {
            url = JacksonJsonParser().parseMap(url)["metaURI"] as String?
                ?: return emptyMeta(itemId)
        }


        if (url.startsWith("ipfs://")) {
            url = url.substring("ipfs:/".length)
        }

        if (url.isEmpty()) {
            return emptyMeta(itemId)
        }

        val client = WebClient.create("https://rarible.mypinata.cloud/")
        return try {
            val data = client.get().uri(url)
                .retrieve().awaitBodyOrNull<RaribleNFTMetaBody>() ?: return emptyMeta(itemId)
            ItemMeta(
                itemId = itemId,
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
            logger.warn(e.message, e)
            return emptyMeta(itemId)
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
