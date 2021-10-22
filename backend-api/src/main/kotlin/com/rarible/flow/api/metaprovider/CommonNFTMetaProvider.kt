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
class CommonNFTMetaProvider(
    private val itemRepository: ItemRepository
) : ItemMetaProvider {

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.contains("CommonNFT")

    override suspend fun getMeta(itemId: ItemId): ItemMeta? {
        val item = itemRepository.findById(itemId).awaitSingleOrNull() ?: return null
        val metaMap = JacksonJsonParser().parseMap(item.meta)
        var url = metaMap["metaURI"] ?: return null
        val client = WebClient.create("https://rarible.mypinata.cloud/")
        if (url.toString().startsWith("ipfs://")) {
            url = url.toString().substring("ipfs:/".length)
        }
        val data = client.get().uri(url.toString()).retrieve().awaitBodyOrNull<CommonNFTMetaBody>() ?: return null
        return ItemMeta(
            itemId = itemId,
            name = data.name,
            description = data.description,
            attributes = data.attributes.map { ItemMetaAttribute(
                key = it.key ?: it.traitType!!,
                value = it.value,
                format = null,
                type = null
            ) },
            contentUrls = listOf(data.image),
        ).apply {
            raw = data.toString().toByteArray(charset = Charsets.UTF_8)
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class CommonNFTMetaBody(
    val name: String,
    val description: String,
    val image: String,
    val attributes: List<CommonNftAttr>
)

data class CommonNftAttr(
    val key: String?,
    @get:JsonProperty("trait_type")
    val traitType: String?,
    val value: String?
)
