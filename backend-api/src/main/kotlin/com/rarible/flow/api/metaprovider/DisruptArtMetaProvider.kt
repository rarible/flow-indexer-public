package com.rarible.flow.api.metaprovider

import com.fasterxml.jackson.databind.node.ObjectNode
import com.nftco.flow.sdk.FlowAddress
import com.rarible.core.apm.withSpan
import com.rarible.flow.Contracts
import com.rarible.flow.core.config.AppProperties
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import com.rarible.flow.core.domain.Part
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.log.Log
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.boot.json.JacksonJsonParser
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import kotlin.math.log

@Component
class DisruptArtMetaProvider(
    private val itemRepository: ItemRepository,
    private val webClient: WebClient,
    private val appProperties: AppProperties
) : ItemMetaProvider {

    private val logger by Log()

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.endsWith(".DisruptArt")

    override suspend fun getMeta(itemId: ItemId): ItemMeta {
        return withSpan("DisruptArt::getMeta", "network") {
            val item = itemRepository.coFindById(itemId) ?: return@withSpan emptyMeta(itemId).let {
                logger.warn("DisruptArt::getMeta::Not found item with itemId [${itemId}]")
                it
            }
            if (item.meta.isNullOrBlank()) return@withSpan emptyMeta(itemId).let {
                logger.warn("DisruptArt::getMeta::Item[${itemId}] meta string is empty!")
                it
            }
            val itemMeta = JacksonJsonParser().parseMap(item.meta!!)
            val contentUrl = itemMeta["content"] as String? ?: return@withSpan emptyMeta(itemId).let {
                logger.warn("DisruptArt::getMeta::Item[${itemId}] meta content url is empty!")
                it
            }
            val spec = webClient.get().uri(contentUrl).retrieve().toBodilessEntity().awaitSingle()
            if (spec.headers.contentType == MediaType.IMAGE_JPEG) {
                logger.info("DisruptArt::getMeta::Meta is simple image!")
                ItemMeta(
                    itemId = itemId,
                    name = itemMeta["name"] as String,
                    description = itemMeta["name"] as String,
                    attributes = emptyList(),
                    contentUrls = listOf(contentUrl)
                ).apply {
                    raw = contentUrl.toByteArray()
                }
            } else {
                val metaData = webClient.get().uri(contentUrl).retrieve().awaitBodyOrNull<ObjectNode>()
                    ?: return@withSpan emptyMeta(itemId)
                val contents = metaData.get("Media").findValue("uri").asText()

                val attributes = mutableListOf<ItemMetaAttribute>()
                metaData.fields().forEach {
                    if (it.key !in setOf("PlatformInfo", "Media", "MediaPreview", "tags")) {
                        attributes.add(ItemMetaAttribute(key = it.key, value = it.value.asText()))
                    }
                }
                logger.info("DisruptArt::getMeta::Meta is JSON!")
                updateRoyalties(item, metaData)
                ItemMeta(
                    itemId = itemId,
                    name = itemMeta["name"] as String,
                    description = metaData.findValue("Description").textValue(),
                    attributes = attributes.toList(),
                    contentUrls = listOf(contents),
                ).apply {
                    raw = metaData.toPrettyString().toByteArray()
                }
            }
        }
    }

    suspend fun updateRoyalties(item: Item, metaData: ObjectNode) {
        val royalties = mutableListOf<Part>()
        val royaltiesJson = metaData.get("royalties")
        royaltiesJson?.iterator()?.forEachRemaining {
            royalties.add(
                Part(
                    FlowAddress(it["address"].asText()),
                    it["fee"].asDouble()
                )
            )
        }
        logger.info("Saving royalties for item {}: {}", item.id, royalties)

        itemRepository.coSave(
            item.copy(royalties = Contracts.DISRUPT_ART.staticRoyalties(appProperties.chainId) + royalties)
        )
    }
}
