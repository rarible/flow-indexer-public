package com.rarible.flow.api.meta.provider.legacy

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.rarible.flow.api.config.Config
import com.rarible.flow.api.meta.ItemMeta
import com.rarible.flow.api.meta.ItemMetaAttribute
import com.rarible.flow.api.meta.ItemMetaContent
import com.rarible.flow.api.meta.provider.ItemMetaProvider
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.util.Log
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull

@Component
class MugenNFTMetaProvider(
    @Qualifier("mugenClient")
    private val mugenClient: WebClient
) : ItemMetaProvider {

    private val logger by Log()

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.contains("MugenNFT")

    override suspend fun getMeta(item: Item): ItemMeta? {
        return try {
            val data = mugenClient.get().uri("/${item.tokenId}")
                .retrieve().awaitBodyOrNull<List<MugenNFTMetaBody>>()?.singleOrNull() ?: return emptyMeta(item.id)
            ItemMeta(
                itemId = item.id,
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
                originalMetaUri = Config.MUGEN_ART_BASE_URL + "/${item.tokenId}",
                externalUri = data.externalUrl,
                content = listOfNotNull(
                    data.imagePreview?.let {
                        ItemMetaContent(
                            it,
                            ItemMetaContent.Type.IMAGE,
                            ItemMetaContent.Representation.PREVIEW,
                        )
                    },
                    data.image?.let {
                        ItemMetaContent(
                            it,
                            ItemMetaContent.Type.IMAGE,
                        )
                    },
                    data.imageHd?.let {
                        ItemMetaContent(
                            it,
                            ItemMetaContent.Type.IMAGE,
                            ItemMetaContent.Representation.BIG,
                        )
                    },
                    data.animationUrl?.let {
                        ItemMetaContent(
                            it,
                            ItemMetaContent.Type.VIDEO,
                        )
                    },
                    data.animationUrl2?.let {
                        ItemMetaContent(
                            it,
                            ItemMetaContent.Type.VIDEO,
                        )
                    },
                ),
            ).apply {
                raw = data.toString().toByteArray(charset = Charsets.UTF_8)
            }
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
    @get:JsonProperty("youtube_url")
    val youtubeUrl: String? = null,
    val name: String,
    @get:JsonProperty("image_blocto")
    val imageBlocto: String?,
) : MetaBody {
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
