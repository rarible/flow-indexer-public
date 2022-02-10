package com.rarible.flow.api.metaprovider

import com.fasterxml.jackson.annotation.JsonProperty
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.cadence.StringField
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.domain.*
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
    private val webClient: WebClient,
    private val script: StarlyMetaScript
): ItemMetaProvider {
    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.contains("StarlyCard")

    override suspend fun getMeta(item: Item): ItemMeta? {
        return script.call(item.owner!!, item.tokenId)?.let { starlyId ->
            webClient
                .get()
                .uri("https://starly.io/c/$starlyId.json")
                .retrieve()
                .awaitBodyOrNull<StarlyMeta>()
        }?.toItemMeta(item.id)
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

): MetaBody {
    override fun toItemMeta(itemId: ItemId): ItemMeta {
        return ItemMeta(
            itemId, title, description,
            listOf(
                ItemMetaAttribute("creator", creator.name),
                ItemMetaAttribute("collection", collection.title),
                ItemMetaAttribute("rarity", rarity),
                ItemMetaAttribute("edition", edition),
                ItemMetaAttribute("editions", editions),
            ),
            mediaSizes.sortedByDescending { it.width }.flatMap {
                listOfNotNull(it.screenshot, it.url)
            }
        )
    }
}

data class StarlyCreator(
    val id: String,
    val name: String
)

data class StarlyCollection(
    val id: String,
    val title: String
)

data class StarlyMedia(
    val width: Int,
    val height: Int,
    val url: String,
    val screenshot: String?
)
