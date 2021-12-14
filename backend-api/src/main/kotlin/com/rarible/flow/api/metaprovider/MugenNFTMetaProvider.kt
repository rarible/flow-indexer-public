package com.rarible.flow.api.metaprovider

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import com.rarible.flow.log.Log
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull

@Component
class MugenNFTMetaProvider : ItemMetaProvider {

    private val logger by Log()

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.contains("MugenNFT")

    override suspend fun getMeta(itemId: ItemId): ItemMeta {
        val url = "https://onchain.mugenart.io/flow/nft/0x2cd46d41da4ce262/metadata/${itemId.tokenId}"

        val client = WebClient.create()
        return try {
            val data = client.get().uri(url)
                .retrieve().awaitBodyOrNull<List<MugenNFTMetaBody>>()?.singleOrNull() ?: return emptyMeta(itemId)
            ItemMeta(
                itemId = itemId,
                name = data.name,
                description = data.description,
                attributes = data.attributes.map {
                    ItemMetaAttribute(
                        key = it.traitType,
                        value = it.value
                    )
                } + listOf(
                    ItemMetaAttribute(
                        key = "backgroundColor",
                        value = data.backgroundColor,
                    ),
                    ItemMetaAttribute(
                        key = "baiduModelKey",
                        value = data.baiduModelKey,
                    )
                ),
                contentUrls = listOfNotNull(
                    data.imageBlocto,
                    data.icon,
                    data.image,
                    data.imagePreview,
                    data.imageHd,
                    data.externalUrl,
                    data.animationUrl,
                    data.animationUrl2,
                    data.youtubeUrl,
                ),
            ).apply {
                raw = data.toString().toByteArray(charset = Charsets.UTF_8)
            }
        } catch (e: Exception) {
            logger.warn("getMeta: ${e.message}", e)
            return emptyMeta(itemId)
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
)

data class MugenNftAttr(
    @get:JsonProperty("trait_type")
    val traitType: String,
    val value: String?,
)
