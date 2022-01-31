package com.rarible.flow.api.metaprovider

import com.fasterxml.jackson.annotation.JsonProperty
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.cadence.StringField
import com.rarible.flow.api.metaprovider.body.MetaBody
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.withEntity
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
    private val itemRepository: ItemRepository,
    private val webClient: WebClient,
    private val script: StarlyMetaScript
): ItemMetaProvider {
    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.contains("StarlyCard")

    override suspend fun getMeta(itemId: ItemId): ItemMeta {
        return itemRepository.withEntity(itemId) { item ->
            script.call(item.owner!!, item.tokenId)
        }?.let { starlyId ->
            webClient
                .get()
                .uri("https://starly.io/c/$starlyId.json")
                .retrieve()
                .awaitBodyOrNull<StarlyMeta>()
        }?.toItemMeta(itemId) ?: ItemMeta.empty(itemId)
    }
}

data class StarlyMeta(
    val title: String,
    val creator: StarlyCreator,
    val collection: StarlyCreator,
    val description: String,
    val rarity: String,

    @get:JsonProperty("media_sizes")
    val mediaSizes: List<StarlyMedia>

): MetaBody {
    override fun toItemMeta(itemId: ItemId): ItemMeta {
        return ItemMeta(
            itemId, title, description,
            listOf(
                ItemMetaAttribute("creator", creator.name),
                ItemMetaAttribute("collection", collection.name),
                ItemMetaAttribute("rarity", rarity)
            ),
            mediaSizes.sortedByDescending { it.width }.map { it.url }.sorted()
        )
    }
}

data class StarlyCreator(
    val id: String,
    val name: String
)

data class StarlyMedia(
    val width: Int,
    val height: Int,
    val url: String
)