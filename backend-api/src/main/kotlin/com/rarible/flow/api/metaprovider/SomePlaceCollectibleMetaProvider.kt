package com.rarible.flow.api.metaprovider

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.CadenceNamespace
import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.JsonCadenceConversion
import com.nftco.flow.sdk.cadence.JsonCadenceConverter
import com.rarible.flow.Contracts
import com.rarible.flow.api.metaprovider.body.MetaBody
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coFindById
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class SomePlaceCollectibleMetaProvider(
    @Value("\${app.chain-id}")
    private val chainId: FlowChainId,
    private val itemRepository: ItemRepository,
    @Value("classpath:script/spc_meta.cdc")
    private val script: Resource,
    private val scriptExecutor: ScriptExecutor
): ItemMetaProvider {
    override fun isSupported(itemId: ItemId): Boolean =
        itemId.contract == Contracts.SOME_PLACE_COLLECTIBLE.fqn(chainId)

    override suspend fun getMeta(itemId: ItemId): ItemMeta {
        return itemRepository.coFindById(itemId).let {
            if (it == null) emptyMeta(itemId)
            getMeta(it!!)
        }
    }

    suspend fun getMeta(item: Item): ItemMeta {
        if (item.owner == null) return emptyMeta(item.id)

        return scriptExecutor.executeFile(
            script,
            {
                arg { address(item.owner!!.formatted) }
                arg { uint64(item.tokenId) }
            },
            { response ->
                optional(response) {
                    Flow.unmarshall(Meta::class, it)
                }
            }
        )?.toItemMeta(item.id) ?: emptyMeta(item.id)
    }

    @JsonCadenceConversion(MetaParser::class)
    internal data class Meta(
        val id: Long,
        val title: String,
        val description: String,
        val mediaUrl: String,
        val attributes: Map<String, String>,
    ): MetaBody {
        override fun toItemMeta(itemId: ItemId): ItemMeta {
            return ItemMeta(
                itemId = itemId,
                name = title,
                description = description,
                attributes = attributes.map {
                    ItemMetaAttribute(it.key, it.value)
                },
                contentUrls = listOf(mediaUrl),
                content = listOf(
                    ItemMeta.Content(
                        url = mediaUrl,
                        type = ItemMeta.Content.Type.IMAGE,
                    )
                )
            ).apply {
                raw = jacksonObjectMapper().writeValueAsBytes(this)
            }
        }
    }

    internal class MetaParser: JsonCadenceConverter<Meta> {
        override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): Meta = com.nftco.flow.sdk.cadence.unmarshall(value) {
            Meta(
                id = long("id"),
                title = string("title"),
                description = string("description"),
                mediaUrl = string("mediaUrl"),
                attributes = dictionaryMap("attributes") { k, v ->
                    string(k) to string(v)
                }
            )
        }
    }
}
