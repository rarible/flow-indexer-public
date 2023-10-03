package com.rarible.flow.api.meta.provider.legacy

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.rarible.flow.Contracts
import com.rarible.flow.api.config.Config
import com.rarible.flow.api.meta.ItemMeta
import com.rarible.flow.api.meta.ItemMetaAttribute
import com.rarible.flow.api.meta.ItemMetaContent
import com.rarible.flow.api.meta.provider.ItemMetaProvider
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.util.Log
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull

@Component
class MatrixWorldMetaProvider(
    private val ipfsClient: WebClient
) : ItemMetaProvider {

    private val logger by Log()

    override fun isSupported(itemId: ItemId): Boolean = Contracts.MATRIX_WORLD_VOUCHER.supports(itemId)

    override suspend fun getMeta(item: Item): ItemMeta? {
        return try {
            ipfsClient
                .get()
                .uri("/${item.tokenId}")
                .retrieve()
                .awaitBodyOrNull<MatrixWorldMetaBody>()
                ?.toItemMeta(item.id)
        } catch (e: Exception) {
            logger.warn("MatrixWorldMetaProvider.getMeta: ${e.message}", e)
            null
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class MatrixWorldMetaBody(
    val image: String? = null,
    val name: String,
    val description: String,
    val attributes: List<MatrixWorldAttr>,

    @get:JsonProperty("animation_url")
    val animationUrl: String? = null
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
            },
            contentUrls = listOfNotNull(
                image,
                animationUrl
            ),
            content = listOfNotNull(
                animationUrl?.let {
                    ItemMetaContent(
                        it,
                        ItemMetaContent.Type.VIDEO,
                    )
                },
                image?.let {
                    ItemMetaContent(
                        it,
                        ItemMetaContent.Type.IMAGE,
                    )
                },
            ),
            originalMetaUri = "${Config.MATRIX_WORLD_BASE_URL}/${itemId.tokenId}"
        ).apply {
            raw = toString().toByteArray(charset = Charsets.UTF_8)
        }
    }
}

data class MatrixWorldAttr(
    @get:JsonProperty("trait_type")
    val traitType: String,
    val value: String?,
)
