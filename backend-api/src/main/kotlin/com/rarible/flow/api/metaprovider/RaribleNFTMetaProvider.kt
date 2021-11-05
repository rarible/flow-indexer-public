package com.rarible.flow.api.metaprovider

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import com.rarible.flow.core.repository.ItemRepository
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.boot.json.JacksonJsonParser
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull

@Component
class RaribleNFTMetaProvider(
    private val itemRepository: ItemRepository
) : ItemMetaProvider {

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.contains("RaribleNFT")

    override suspend fun getMeta(itemId: ItemId): ItemMeta? {
        val item = itemRepository.findById(itemId).awaitSingleOrNull() ?: throw IllegalStateException("Not found item by id [$itemId]")
        var url = item.meta ?: throw IllegalStateException("meta is null")
        if (url.startsWith("{")) {
            url = JacksonJsonParser().parseMap(url)["metaURI"] as String? ?: throw IllegalStateException("metaURI not found")
        }

        val client = WebClient.create("https://rarible.mypinata.cloud/")
        if (url.startsWith("ipfs://")) {
            url = url.substring("ipfs:/".length)
        }
        val data = client.get().uri(url).retrieve().awaitBodyOrNull<RaribleNFTMetaBody>() ?: throw IllegalStateException("Cant get meta from ipfs")
        return ItemMeta(
            itemId = itemId,
            name = data.name,
            description = data.description,
            attributes = data.attributes.map { ItemMetaAttribute(
                key = it.key ?: it.traitType!!,
                value = it.value
            ) },
            contentUrls = listOfNotNull(data.image, data.animationUrl),
        ).apply {
            raw = data.toString().toByteArray(charset = Charsets.UTF_8)
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
