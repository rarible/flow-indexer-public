package com.rarible.flow.api.metaprovider

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coFindById
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.boot.json.JacksonJsonParser
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull

@Component
class DisruptArtMetaProvider(
    private val itemRepository: ItemRepository
) : ItemMetaProvider {
    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.endsWith("DisruptArt")

    override suspend fun getMeta(itemId: ItemId): ItemMeta {
        val item = itemRepository.coFindById(itemId) ?: return emptyMeta(itemId)
        if (item.meta.isNullOrBlank()) return emptyMeta(itemId)
        val itemMeta = JacksonJsonParser().parseMap(item.meta!!)
        val contentUrl = itemMeta["content"] as String? ?: return emptyMeta(itemId)
        val client = WebClient.create(contentUrl)
        val spec = client.get().retrieve().toBodilessEntity().awaitSingle()
        if (spec.headers.contentType == MediaType.IMAGE_JPEG) {
            return ItemMeta(
                itemId = itemId,
                name = itemMeta["name"] as String,
                description = itemMeta["name"] as String,
                attributes = emptyList(),
                contentUrls = listOf(contentUrl)
            ).apply {
                raw = contentUrl.toByteArray()
            }
        } else {
            val metaData = client.get().retrieve().awaitBodyOrNull<ObjectNode>() ?: return emptyMeta(itemId)
            val contents = metaData.get("Media").findValue("uri").asText()

            val attributes = mutableListOf<ItemMetaAttribute>()
            metaData.fields().forEach {
                if (it.key !in setOf("PlatformInfo", "Media", "MediaPreview", "tags")) {
                    attributes.add(ItemMetaAttribute(key = it.key, value = it.value.asText()))
                }
            }
            return ItemMeta(
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
