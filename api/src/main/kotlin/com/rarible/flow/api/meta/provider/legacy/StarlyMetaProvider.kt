package com.rarible.flow.api.meta.provider.legacy

import com.fasterxml.jackson.annotation.JsonProperty
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.cadence.StringField
import com.rarible.flow.api.meta.ItemMeta
import com.rarible.flow.api.meta.ItemMetaAttribute
import com.rarible.flow.api.meta.ItemMetaContent
import com.rarible.flow.api.meta.provider.ItemMetaProvider
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.TokenId
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull

@Component
class StarlyMetaScript(
    private val scriptExecutor: ScriptExecutor,
    @Value("classpath:script/starly_meta.cdc")
    private val script: Resource
) {
    suspend fun call(owner: FlowAddress, tokenId: TokenId): String? {
        return scriptExecutor.executeFile(
            script,
            {
                arg { address(owner.formatted) }
                arg { uint64(tokenId) }
            },
            { json ->
                json.value?.let {
                    (it as StringField).value
                }
            }
        )
    }
}

@Component
class StarlyMetaProvider(
    private val ipfsClient: WebClient,
    private val script: StarlyMetaScript
) : ItemMetaProvider {
    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.contains("StarlyCard")

    override suspend fun getMeta(item: Item): ItemMeta? {
        return script.call(item.owner!!, item.tokenId)?.let { starlyId ->
            val uri = "https://starly.io/c/$starlyId.json"
            uri to ipfsClient
                .get()
                .uri(uri)
                .retrieve()
                .awaitBodyOrNull<StarlyMeta>()
        }?.let { (uri, starlyMeta) ->
            starlyMeta?.toItemMeta(item.id)?.copy(originalMetaUri = uri)
        } ?: ItemMeta.empty(item.id)
    }
}

data class StarlyMeta(
    val title: String,
    val creator: StarlyCreator,
    val collection: StarlyCollection,
    val description: String,
    val rarity: String,

    @get:JsonProperty("media_sizes")
    val mediaSizes: List<StarlyMedia>,
    val edition: String,
    val editions: String,

) : MetaBody {

    override fun toItemMeta(itemId: ItemId): ItemMeta {
        return ItemMeta(
            itemId = itemId,
            name = title,
            description = description,
            attributes = listOf(
                ItemMetaAttribute("creator", creator.name),
                ItemMetaAttribute("collection", collection.title),
                ItemMetaAttribute("rarity", rarity),
                ItemMetaAttribute("edition", edition),
                ItemMetaAttribute("editions", editions),
            ),
            contentUrls = mediaSizes.sortedByDescending { it.width }.flatMap {
                listOfNotNull(it.screenshot, it.url)
            },
            content = mediaSizes.sortedBy { it.width }.mapIndexed { index, media ->
                when {
                    mediaSizes.size > 1 && index == 0 -> media.asContent()
                        .copy(representation = ItemMetaContent.Representation.PREVIEW)
                    mediaSizes.size > 2 && index == mediaSizes.size - 1 -> media.asContent()
                        .copy(representation = ItemMetaContent.Representation.BIG)
                    else -> media.asContent()
                }
            },
        )
    }
}

private fun StarlyMedia.asContent() = ItemMetaContent(
    url = url,
    representation = ItemMetaContent.Representation.ORIGINAL,
    type = ItemMetaContent.Type.IMAGE,
    width = width,
    height = height,
)

data class StarlyCreator(
    val id: String,
    val name: String,
)

data class StarlyCollection(
    val id: String,
    val title: String,
)

data class StarlyMedia(
    val width: Int,
    val height: Int,
    val url: String,
    val screenshot: String?
)
