package com.rarible.flow.api.metaprovider

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.rarible.flow.Contracts
import com.rarible.flow.api.config.ApiProperties
import com.rarible.flow.api.metaprovider.body.MetaBody
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import com.rarible.flow.log.Log
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Component
class FanfareMetaProvider(
    private val webClient: WebClient,
    private val apiProperties: ApiProperties
): ItemMetaProvider {

    val logger by Log()

    override fun isSupported(itemId: ItemId): Boolean =
        itemId.contract == Contracts.FANFARE.fqn(apiProperties.chainId)

    override suspend fun getMeta(itemId: ItemId): ItemMeta {
        val tokenId = itemId.tokenId

        return try {
            webClient
                .get()
                .uri("https://www.fanfare.fm/api/nft-meta/{id}", mapOf("id" to tokenId))
                .retrieve()
                .awaitBodyOrNull<FanfareMeta>()
        } catch (e: Throwable) {
            logger.warn("Failed to fetch meta of {}", itemId, e)
            null
        }?.toItemMeta(itemId) ?: ItemMeta.empty(itemId)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class FanfareMeta(
    val name: String,
    val description: String,
    val attributes: List<FanfareAttribute>,
    @get:JsonProperty("audio_url")
    val audioUrl: String,
    val image: String,

): MetaBody {
    override fun toItemMeta(itemId: ItemId): ItemMeta {
        return ItemMeta(
            itemId, name, description,
            attributes.map(FanfareAttribute::toItemAttribute),
            listOf(
                image, audioUrl
            )
        )
    }
}

data class FanfareAttribute(
    @get:JsonProperty("display_type")
    val displayType: String?,
    @get:JsonProperty("trait_type")
    val traitType: String?,
    val value: String?
) {
    fun toItemAttribute(): ItemMetaAttribute {
        return if(displayType == "date") {
            ItemMetaAttribute(
                traitType!!,
                DateTimeFormatter.ISO_LOCAL_DATE.format(
                    Instant.ofEpochSecond(value!!.toLong()).atOffset(ZoneOffset.UTC)
                )
            )
        } else ItemMetaAttribute(traitType!!, value!!)
    }
}