package com.rarible.flow.api.metaprovider

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import com.rarible.flow.log.Log
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull

@Component
class MugenNFTMetaProvider(
    private val mugenCient: WebClient
) : ItemMetaProvider {

    private val logger by Log()

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.contains("MugenNFT")

    override suspend fun getMeta(item: Item): ItemMeta? {
        return try {
            return mugenCient
                .get()
                .uri("/${item.tokenId}")
                .retrieve()
                .awaitBodyOrNull<List<MugenNFTMetaBody>>()
                ?.firstOrNull()
                ?.toItemMeta(item.id)
        } catch (e: Exception) {
            logger.warn("getMeta: ${e.message}", e)
            null
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class MugenNFTMetaBody(
    val attributes: List<MugenNftAttr>,
    val address: String,
    @get:JsonProperty("type_id")
    val typeId: Long,
    @get:JsonProperty("token_id")
    val tokenId: Long,
    val description: String,
    @get:JsonProperty("external_url")
    val externalUrl: String? = null,
    val icon: String? = null,
    val image: String? = null,
    @get:JsonProperty("image_preview")
    val imagePreview: String? = null,
    @get:JsonProperty("image_hd")
    val imageHd: String? = null,
    @get:JsonProperty("background_color")
    val backgroundColor: String? = null,
    @get:JsonProperty("animation_url")
    val animationUrl: String? = null,
    @get:JsonProperty("animation_url_2")
    val animationUrl2: String? = null,
    @get:JsonProperty("baidu_model_key")
    val baiduModelKey: String? = null,
    @get:JsonProperty("youtube_url")
    val youtubeUrl: String? = null,
    val name: String,
    @get:JsonProperty("image_blocto")
    val imageBlocto: String?,
): MetaBody {
    override fun toItemMeta(itemId: ItemId): ItemMeta {
        return ItemMeta(
            itemId = itemId,
            name = name,
            description = description,
            attributes = attributes.map {
                ItemMetaAttribute(
                    key = it.traitType,
                    value = it.value
                )
            } + listOf(
                ItemMetaAttribute(
                    key = "backgroundColor",
                    value = backgroundColor,
                ),
                ItemMetaAttribute(
                    key = "baiduModelKey",
                    value = baiduModelKey,
                )
            ),
            contentUrls = listOfNotNull(
                imageBlocto,
                icon,
                image,
                imagePreview,
                imageHd,
                externalUrl,
                animationUrl,
                animationUrl2,
                youtubeUrl,
            ),
        ).apply {
            raw = toString().toByteArray(charset = Charsets.UTF_8)
        }
    }
}

data class MugenNftAttr(
    @get:JsonProperty("trait_type")
    val traitType: String,
    val value: String?,
)
