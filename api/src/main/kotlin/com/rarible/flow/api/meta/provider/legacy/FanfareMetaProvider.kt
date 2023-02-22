package com.rarible.flow.api.meta.provider.legacy

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.cadence.StringField
import com.rarible.flow.Contracts
import com.rarible.flow.api.config.ApiProperties
import com.rarible.flow.api.meta.ItemMeta
import com.rarible.flow.api.meta.ItemMetaAttribute
import com.rarible.flow.api.meta.ItemMetaContent
import com.rarible.flow.api.meta.provider.ItemMetaProvider
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.TokenId
import org.springframework.stereotype.Component

@Component
class FanfareMetaProvider(
    private val apiProperties: ApiProperties,
    private val scriptExecutor: ScriptExecutor,
) : ItemMetaProvider {

    private val objectMapper = jacksonObjectMapper()

    override fun isSupported(itemId: ItemId): Boolean = Contracts.FANFARE.supports(itemId)

    override suspend fun getMeta(item: Item): ItemMeta? {
        val itemId = item.id

        if (item.owner == null) return emptyMeta(itemId)
        val metaString = (if (item.meta.isNullOrBlank()) getFlowMeta(item.owner!!, item.tokenId) else item.meta)
            ?: return emptyMeta(itemId)
        val metaWrap = objectMapper.readValue<FanfareMetaWrap>(metaString).metadata
        val meta = objectMapper.readValue<FanfareMeta>(metaWrap)
        return meta.toItemMeta(itemId)
    }

    private suspend fun getFlowMeta(owner: FlowAddress, tokenId: TokenId) =
        scriptExecutor.executeFile("classpath:script/item/item_fanfare.cdc", {
            arg { address(owner) }
            arg { uint64(tokenId) }
        }, { json ->
            json.value?.let { (it as StringField).value }
        })
}

data class FanfareMetaWrap(val metadata: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class FanfareMeta(
    @get:JsonProperty("artist_name") val artistName: String,
    val title: String,
    val description: String,
    val genre: String,
    @get:JsonProperty("external_url") val externalUrl: String,
    @get:JsonProperty("image_url") val imageUrl: String,
    @get:JsonProperty("audio_url") val audioUrl: String,
    @get:JsonProperty("is_music_video") val isMusicVideo: Boolean,
    @get:JsonProperty("total_copies") val totalCopies: Int,
    val edition: Int,

    ) : MetaBody {
    override fun toItemMeta(itemId: ItemId): ItemMeta {
        return ItemMeta(
            itemId = itemId,
            name = "$title //$artistName",
            description = description,
            attributes = listOf(
                ItemMetaAttribute(key = "genre", value = genre),
                ItemMetaAttribute(key = "is_music_video", value = isMusicVideo.toString()),
                ItemMetaAttribute(key = "total_copies", value = totalCopies.toString()),
                ItemMetaAttribute(key = "edition", value = edition.toString()),
            ),
            contentUrls = listOf(imageUrl, audioUrl, externalUrl),
            genres = listOfNotNull(genre.takeIf { it.isNotEmpty() }).takeIf { it.isNotEmpty() },
            content = listOf(
                ItemMetaContent(
                    imageUrl,
                    if (isMusicVideo) ItemMetaContent.Type.VIDEO else ItemMetaContent.Type.IMAGE
                ),
                ItemMetaContent(
                    audioUrl,
                    ItemMetaContent.Type.AUDIO
                )
            ),
            externalUri = externalUrl,
        )
    }
}
