package com.rarible.flow.api.metaprovider

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rarible.core.apm.withSpan
import com.rarible.flow.Contracts
import com.rarible.flow.core.config.AppProperties
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import java.time.Instant
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull

@Component
class CryptoPiggoMetaProvider(
    private val webClient: WebClient,
    private val appProperties: AppProperties
): ItemMetaProvider {

    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    private val metaUrl = "https://rareworx.com/client-server/v1/piggos/{id}"

    override fun isSupported(itemId: ItemId): Boolean =
        itemId.contract == Contracts.CRYPTOPIGGO.fqn(appProperties.chainId)

    override suspend fun getMeta(item: Item): ItemMeta {
        return withSpan("CryptoPiggoMetaProvider::getMeta", "network") {
            val piggoMeta = webClient.get().uri(metaUrl, item.tokenId)
                .retrieve().awaitBodyOrNull<PiggoItem>() ?: return@withSpan emptyMeta(item.id)

            return@withSpan ItemMeta(
                itemId = item.id,
                name = "Cryptopiggo #${item.tokenId}",
                description = "",
                attributes = extractAttributes(piggoMeta),
                contentUrls = listOf(piggoMeta.file.url),
                content = listOf(
                    ItemMeta.Content(
                        piggoMeta.file.url,
                        ItemMeta.Content.Representation.ORIGINAL,
                        ItemMeta.Content.Type.IMAGE,
                    )
                ),
                createdAt = piggoMeta.createdAt,
                originalMetaUri = metaUrl.replace("{id}", itemId.tokenId.toString()),
            ).apply {
                raw = mapper.writeValueAsBytes(piggoMeta)
            }
        }
    }

    private fun resolveRarity(rarity: Int): String = when(rarity) {
        1 -> "Mythic"
        2,3 -> "Legendary"
        4,5 -> "Ultra Rare"
        6,7 -> "Rare"
        8,9 -> "Uncommon"
        else -> "Common"
    }

    private fun extractAttributes(piggoMeta: PiggoItem): List<ItemMetaAttribute> =
        listOf(
            ItemMetaAttribute(
                key = capitalize(PiggoItem::rarity.name),
                value = resolveRarity(piggoMeta.rarity)
            ),
            ItemMetaAttribute(
                key = capitalize(PiggoItem::status.name),
                value = piggoMeta.status.status
            ),
            ItemMetaAttribute(
                key = capitalize(PiggoItem::background.name),
                value = piggoMeta.background
            ),
            ItemMetaAttribute(
                key = capitalize(PiggoItem::type.name),
                value = piggoMeta.type
            ),
            ItemMetaAttribute(
                key = capitalize(PiggoItem::pants.name),
                value = piggoMeta.pants
            ),
            ItemMetaAttribute(
                key = capitalize(PiggoItem::shirts.name),
                value = piggoMeta.shirts
            ),
            ItemMetaAttribute(
                key = capitalize(PiggoItem::expression.name),
                value = piggoMeta.expression,
            ),
            ItemMetaAttribute(
                key = capitalize(PiggoItem::beard.name),
                value = piggoMeta.beard
            ),
            ItemMetaAttribute(
                key = capitalize(PiggoItem::head.name),
                value = piggoMeta.head
            ),
            ItemMetaAttribute(
                key = capitalize(PiggoItem::accessories.name),
                value = piggoMeta.accessories
            )
        )

    private fun capitalize(str: String): String = str.replaceFirstChar { it.titlecase() }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class PiggoItem(
    val id: Long,
    val createdAt: Instant,
    val hash: String,
    val tag: String,
    val background: String,
    val type: String,
    @JsonProperty("clothingBottoms")
    val pants: String,
    @JsonProperty("clothingTops")
    val shirts: String,
    val expression: String,
    val beard: String,
    val head: String,
    val accessories: String,
    val rarity: Int,
    val fileId: String,
    val file: PiggoFile,
    @JsonProperty("piggoStatus")
    val status: PiggoStatus
)

data class PiggoFile(
    val id: String, val createdAt: Instant, val key: String, val url: String
)

data class PiggoStatus(
    val id: Long, val createdAt: Instant, val updatedAt: Instant, val status: String
)
