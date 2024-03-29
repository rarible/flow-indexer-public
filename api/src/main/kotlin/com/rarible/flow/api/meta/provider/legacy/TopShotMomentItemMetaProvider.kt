package com.rarible.flow.api.meta.provider.legacy

import com.jayway.jsonpath.TypeRef
import com.netflix.graphql.dgs.client.MonoGraphQLClient
import com.rarible.flow.api.meta.ItemMeta
import com.rarible.flow.api.meta.ItemMetaAttribute
import com.rarible.flow.api.meta.provider.ItemMetaProvider
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.json.JacksonJsonParser
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class TopShotMomentItemMetaProvider(
    @Value("classpath:script/topshot_play_meta.cdc")
    private val scriptFile: Resource,
    private val scriptExecutor: ScriptExecutor,
) : ItemMetaProvider {

    private val nbaTopShotApiUrl = "https://public-api.nbatopshot.com/graphql"

    private val logger: Logger = LoggerFactory.getLogger(TopShotMomentItemMetaProvider::class.java)

    private val staticNames = setOf(
        "Animated_1080_1080_Black.mp4",
        "Animated_1080_1080_Texture.mp4",
        "Animated_1080_1920_Black.mp4",
        "Animated_1080_1920_Texture.mp4",
        "Hero_2880_2880_Black.jpg",
        "ReverseHero_2880_2880_Black.jpg",
        "Category_2880_2880_Black.jpg",
        "Game_2880_2880_Black.jpg",
        "Logos_2880_2880_Black.jpg",
        "NFT_2880_2880_Black.jpg",
        "Player_2880_2880_Black.jpg",
        "Hero_2400_1200_Texture.jpg",
        "Hero_2400_2400_Texture.jpg",
        "Hero_375_375_Transparent.png"
    )

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.contains("TopShot")

    override suspend fun getMeta(item: Item): ItemMeta? {
        if (item.meta.isNullOrEmpty()) return null
        val metaMap = JacksonJsonParser().parseMap(item.meta)
        val playID = metaMap["playID"].toString()
        val setID = metaMap["setID"].toString()
        val playMap = scriptExecutor.executeFile(scriptFile, {
            arg { uint32(playID) }
            arg { uint32(setID) }
        }, { json ->
            dictionaryMap(json) { k, v ->
                string(k) to string(v)
            }
        })

        val attrs = playMap.filterKeys { it != "FullName" }.map { e ->
            ItemMetaAttribute(
                key = e.key,
                value = e.value,
                type = if (e.key.contains("date", true)) {
                    "string"
                } else null,
                format = if (e.key.contains("date", true)) {
                    "date-time"
                } else null
            )
        }.toMutableList()

        val graphQLData = doQuery(item.tokenId)
        return ItemMeta(
            itemId = item.id,
            name = playMap["FullName"]!!,
            description = graphQLData?.first.orEmpty(),
            contentUrls = graphQLData?.second ?: emptyList(),
            attributes = attrs
        ).apply {
            raw = toString().toByteArray(charset = Charsets.UTF_8)
        }
    }

    private suspend fun doQuery(momentID: Long): Pair<String, List<String>>? {
        val client = MonoGraphQLClient.createWithWebClient(WebClient.create(nbaTopShotApiUrl))
        val resp = client.reactiveExecuteQuery(
            query = """
                     query getMintedMoment (${'$'}momentId: ID!) {
                            getMintedMoment (momentId: ${'$'}momentId) {
                                data {
                                    play {
                                        id
                                        description
                                    }
                                    assetPathPrefix
                                }
                            }
                        }
            """.trimIndent(),
            variables = mapOf("momentId" to momentID)
        ).awaitSingleOrNull() ?: return null
        if (resp.hasErrors()) {
            logger.warn("Error while fetch data for moment[$momentID]: ${resp.errors.map { it.message }.joinToString { "," }}")
            return null
        }
        val innerId = resp.extractValue<String>("data.getMintedMoment.data.play.id")
        val description = resp.extractValue<String>("data.getMintedMoment.data.play.description")
        val assetPrefix = resp.extractValue<String>("data.getMintedMoment.data.assetPathPrefix")
        val urlsResp = client.reactiveExecuteQuery(
            query = """
                    query getPlay (${'$'}playID: GetPlayInput!) {
                        getPlay (playID:${'$'}playID) {
                            play {
                                id  
                                assets {
                                    videos {
                                        url
                                    }
                                    images {
                                        url
                                    }
                                }
                            }
                        }
                    }
            """.trimIndent(),
            variables = mapOf("playID" to mapOf("playID" to innerId))
        ).awaitSingleOrNull() ?: return Pair(description, emptyList())
        val videos = urlsResp.extractValueAsObject(
            "data.getPlay.play.assets.videos",
            object : TypeRef<List<Map<String, String>>>() {}
        ).mapNotNull { it["url"] }

        val images = urlsResp.extractValueAsObject(
            "data.getPlay.play.assets.images",
            object : TypeRef<List<Map<String, String>>>() {}
        ).mapNotNull { it["url"] }

        return Pair(description, staticNames.map { "${assetPrefix}$it" } + images + videos)
    }
}
