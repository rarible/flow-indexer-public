package com.rarible.flow.api.metaprovider

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.log.Log
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.boot.json.JacksonJsonParser
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.kotlin.extra.retry.retryExponentialBackoff
import java.time.Duration

@Component
class RaribleNFTMetaProvider(
    private val pinataClient: WebClient,
    private val itemRepository: ItemRepository
) : ItemMetaProvider {

    private val logger by Log()

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.contains("RaribleNFT")

    override suspend fun getMeta(itemId: ItemId): ItemMeta {
        return try {
            itemRepository.coFindById(itemId)
                ?.let { item -> readUrl(item) }
                ?.let { ipfs ->
                    pinataClient
                        .get()
                        .uri("$ipfs")
                        .retrieve()
                        .bodyToMono(RaribleNFTMetaBody::class.java)
                        .retryExponentialBackoff(3, Duration.ofMillis(500))
                        .awaitFirstOrNull()
                }
                ?.toItemMeta(itemId)
        } catch (e: Exception) {
            logger.warn("Failed RaribleNFTMetaProvider.getMeta({})", itemId, e)
            null
        } ?: ItemMeta.empty(itemId)
    }

    fun readUrl(item: Item): String? {
        var url = item.meta ?: return null
        if (url.startsWith("{")) {
            url = JacksonJsonParser().parseMap(url)["metaURI"] as String?
                ?: return null
        }

        if (url.startsWith("ipfs://")) {
            url = url.substring("ipfs://ipfs".length)
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
) {
    fun toItemMeta(itemId: ItemId): ItemMeta {
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
