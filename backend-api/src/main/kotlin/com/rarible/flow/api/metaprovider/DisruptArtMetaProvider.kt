package com.rarible.flow.api.metaprovider

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.core.apm.withSpan
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import com.rarible.flow.log.Log
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.boot.json.JacksonJsonParser
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull

@Component
class DisruptArtMetaProvider(
    private val webClient: WebClient,
) : ItemMetaProvider {

    private val logger by Log()

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.endsWith(".DisruptArt")

    override suspend fun getMeta(item: Item): ItemMeta? {
        return withSpan("DisruptArt::getMeta", "network") {
            if (item.meta.isNullOrBlank()) return@withSpan null.let {
                logger.warn("DisruptArt::getMeta::Item[${item.id}] meta string is empty!")
                it
            }
            val itemMeta = JacksonJsonParser().parseMap(item.meta!!)
            val contentUrl = itemMeta["content"] as String? ?: return@withSpan null.let {
                logger.warn("DisruptArt::getMeta::Item[${item.id}] meta content url is empty!")
                it
            }
            val spec = webClient.get().uri(contentUrl).retrieve().toBodilessEntity().awaitSingle()
            if (spec.headers.contentType == MediaType.IMAGE_JPEG) {
                logger.info("DisruptArt::getMeta::Meta is simple image!")
                ItemMeta(
                    itemId = item.id,
                    name = itemMeta["name"] as String,
                    description = itemMeta["name"] as String,
                    attributes = emptyList(),
                    contentUrls = listOf(contentUrl)
                ).apply {
                    raw = contentUrl.toByteArray()
                }
            } else {
                val metaData = webClient.get().uri(contentUrl).retrieve().awaitBodyOrNull<ObjectNode>()
                    ?: return@withSpan null
                val contents = metaData.get("Media").findValue("uri").asText()

                val attributes = mutableListOf<ItemMetaAttribute>()
                metaData.fields().forEach {
                    if (it.key !in setOf("PlatformInfo", "Media", "MediaPreview", "tags")) {
                        attributes.add(ItemMetaAttribute(key = it.key, value = it.value.asText()))
                    }
                }
                logger.info("DisruptArt::getMeta::Meta is JSON!")
                ItemMeta(
                    itemId = item.id,
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
}
