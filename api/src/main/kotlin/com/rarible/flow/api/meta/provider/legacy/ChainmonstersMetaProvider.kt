package com.rarible.flow.api.meta.provider.legacy

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.netflix.graphql.dgs.client.WebClientGraphQLClient
import com.rarible.flow.Contracts
import com.rarible.flow.api.config.ApiProperties
import com.rarible.flow.api.config.Config
import com.rarible.flow.api.meta.ItemMeta
import com.rarible.flow.api.meta.ItemMetaAttribute
import com.rarible.flow.api.meta.ItemMetaContent
import com.rarible.flow.api.meta.provider.ItemMetaProvider
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.util.Log
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.boot.json.JacksonJsonParser
import org.springframework.stereotype.Component

@Component
class ChainmonstersMetaProvider(
    private val itemRepository: ItemRepository,
    private val chainMonstersGraphQl: WebClientGraphQLClient,
    private val apiProperties: ApiProperties
) : ItemMetaProvider {

    private val objectMapper = jacksonObjectMapper()

    override fun isSupported(itemId: ItemId): Boolean =
        itemId.contract == Contracts.CHAINMONSTERS.fqn(apiProperties.chainId)

    override suspend fun getMeta(item: Item): ItemMeta? {
        return item.meta
            ?.let { preMeta ->
                JacksonJsonParser().parseMap(preMeta)["rewardId"] as String?
            }?.let { rewardId ->
                chainMonstersGraphQl.reactiveExecuteQuery(getReward(rewardId)).awaitSingleOrNull()
            }?.let { response ->
                if (response.hasErrors()) {
                    logger.warn(
                        "Failed to fetch metadata for {}: {}", item.id, response.errors.joinToString(";") {
                            it.message
                        }
                    )
                    null
                } else {
                    objectMapper
                        .readValue<ChainmonstersData>(response.json)
                        .data
                        .reward
                }
            }?.toItemMeta(item.id)

    }

    companion object {
        fun getReward(rewardId: String): String = """
            query {
              reward(id: $rewardId) {
                id
                name
                desc
                img
                season
              }
            }
        """.trimIndent()

        val logger by Log()
    }
}

data class ChainmonstersData(
    val data: ChainmonstersResponse
)

data class ChainmonstersResponse(
    val reward: ChainmonstersMeta
)

data class ChainmonstersMeta(
    val id: Long,
    val name: String,
    val desc: String,
    val img: String,
    val season: String
) : MetaBody {
    override fun toItemMeta(itemId: ItemId): ItemMeta {
        return ItemMeta(
            itemId = itemId,
            name = name,
            description = desc,
            attributes = listOf(ItemMetaAttribute("season", season)),
            contentUrls = listOf(img),
            content = listOf(
                ItemMetaContent(
                    img,
                    ItemMetaContent.Type.IMAGE,
                    ItemMetaContent.Representation.ORIGINAL,
                )
            ),
            originalMetaUri = Config.CHAIN_MONSTERS_GRAPH_QL,
        )
    }
}
