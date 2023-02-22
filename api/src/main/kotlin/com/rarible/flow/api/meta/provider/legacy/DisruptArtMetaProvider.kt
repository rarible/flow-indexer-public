package com.rarible.flow.api.meta.provider.legacy

import com.fasterxml.jackson.databind.node.ObjectNode
import com.nftco.flow.sdk.FlowAddress
import com.rarible.core.apm.withSpan
import com.rarible.flow.Contracts
import com.rarible.flow.api.meta.ItemMeta
import com.rarible.flow.api.meta.ItemMetaAttribute
import com.rarible.flow.api.meta.ItemMetaContent
import com.rarible.flow.api.meta.provider.ItemMetaProvider
import com.rarible.flow.core.config.AppProperties
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Part
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.core.util.Log
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.boot.json.JacksonJsonParser
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Component
class DisruptArtMetaProvider(
    private val itemRepository: ItemRepository,
    private val ipfsClient: WebClient,
    private val appProperties: AppProperties,
) : ItemMetaProvider {

    private val logger by Log()

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.endsWith(".DisruptArt")

    override suspend fun getMeta(item: Item): ItemMeta? {
        return withSpan("DisruptArt::getMeta", "network") {
            val itemId = item.id
            if (item.meta.isNullOrBlank()) return@withSpan emptyMeta(itemId).let {
                logger.warn("DisruptArt::getMeta::Item[$itemId] meta string is empty!")
                it
            }
            val itemMeta = JacksonJsonParser().parseMap(item.meta!!)
            val contentUrl = itemMeta["content"] as String? ?: return@withSpan emptyMeta(itemId).let {
                logger.warn("DisruptArt::getMeta::Item[$itemId] meta content url is empty!")
                it
            }
            val spec = ipfsClient.get().uri(contentUrl).retrieve().toBodilessEntity().awaitSingle()
            if (spec.headers.contentType == MediaType.IMAGE_JPEG) {
                logger.info("DisruptArt::getMeta::Meta is simple image!")
                ItemMeta(
                    itemId = itemId,
                    name = itemMeta["name"] as String,
                    description = itemMeta["name"] as String,
                    attributes = emptyList(),
                    contentUrls = listOf(contentUrl),
                    content = listOf(
                        ItemMetaContent(
                            contentUrl,
                            ItemMetaContent.Type.IMAGE,
                            ItemMetaContent.Representation.ORIGINAL,
                            mimeType = MediaType.IMAGE_JPEG_VALUE,
                        ),
                    )
                ).apply {
                    raw = contentUrl.toByteArray()
                }
            } else {
                val metaData = ipfsClient.get().uri(contentUrl).retrieve().awaitBodyOrNull<ObjectNode>()
                    ?: return@withSpan emptyMeta(itemId)
                val media = metaData.get("Media").findValue("uri").asText()
                val mediaPreview = metaData.get("MediaPreview").findValue("uri").asText()

                val attributes = mutableListOf<ItemMetaAttribute>()
                metaData.fields().forEach {
                    if (it.key !in setOf(
                            "PlatformInfo", "Media", "MediaPreview", "tags", "Description", "royalties", "MintedDate"
                        )
                    ) {
                        attributes.add(ItemMetaAttribute(key = it.key, value = it.value.asText()))
                    }
                }
                logger.info("DisruptArt::getMeta::Meta is JSON!")
                updateRoyalties(item, metaData)
                val createdAt = runCatching {
                    LocalDate.parse(
                        metaData.findValue("MintedDate").textValue(),
                        DISRUPT_ART_DATE_FORMAT
                    )
                }.getOrNull()?.atStartOfDay(ZoneOffset.UTC)?.toInstant()
                val tags = runCatching {
                    metaData.findValue("tags").toList().mapNotNull { it.asText() }
                }.getOrNull()
                ItemMeta(
                    itemId = itemId,
                    name = itemMeta["name"] as String,
                    description = metaData.findValue("Description").textValue(),
                    attributes = attributes.toList(),
                    contentUrls = listOf(media, mediaPreview),
                    content = listOf(
                        ItemMetaContent(
                            media,
                            ItemMetaContent.Type.IMAGE,
                            ItemMetaContent.Representation.ORIGINAL,
                        ),
                        ItemMetaContent(
                            mediaPreview,
                            ItemMetaContent.Type.IMAGE,
                            ItemMetaContent.Representation.PREVIEW,
                        ),
                    ),
                    tags = tags,
                    createdAt = createdAt,
                    originalMetaUri = contentUrl,
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

        if (royalties.isEmpty()) royalties.addAll(Contracts.DISRUPT_ART.staticRoyalties(appProperties.chainId))
        logger.info("Saving royalties for item {}: {}", item.id, royalties)

        itemRepository.coSave(
            item.copy(royalties = royalties)
        )
    }

    companion object {
        private val DISRUPT_ART_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MMMM-uuuu", Locale.ENGLISH)!!
    }
}
