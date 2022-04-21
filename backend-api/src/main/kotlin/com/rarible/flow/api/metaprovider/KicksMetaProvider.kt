package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.cadence.CadenceNamespace
import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.JsonCadenceConversion
import com.nftco.flow.sdk.cadence.JsonCadenceConverter
import com.nftco.flow.sdk.cadence.OptionalField
import com.rarible.flow.Contracts
import com.rarible.flow.api.metaprovider.body.MetaBody
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coFindById
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class KicksMetaScript(
    private val scriptExecutor: ScriptExecutor,
    @Value("classpath:script/meta/kicks.cdc")
    private val script: Resource
) {
    suspend fun call(owner: FlowAddress, tokenId: TokenId): MetaBody? {
        return scriptExecutor.executeFile(
            script,
            {
                arg { address(owner.formatted) }
                arg { uint64(tokenId) }
            },
            { response ->
                response as OptionalField
                response.value?.let {
                    Flow.unmarshall(KicksMeta::class, it)
                }
            }
        )
    }
}

@Component
class KicksMetaProvider(
    private val itemRepository: ItemRepository,
    private val metaScript: KicksMetaScript
): ItemMetaProvider {

    override fun isSupported(itemId: ItemId): Boolean = Contracts.KICKS.supports(itemId)

    override suspend fun getMeta(itemId: ItemId): ItemMeta {
        return itemRepository
            .coFindById(itemId)
            ?.let { item -> getMeta(item)}
            ?: ItemMeta.empty(itemId)
    }

    suspend fun getMeta(item: Item): ItemMeta? {
        return metaScript
            .call(item.owner ?: item.creator, item.tokenId)
            ?.toItemMeta(item.id)
    }
}

@JsonCadenceConversion(KicksMetaConverter::class)
data class KicksMeta(
    val title: String,
    val description: String,
    val video: String?,
    val image: String?,
    val redeemed: Boolean,
    val size: String?,
    val taggedTopShot: String?
): MetaBody {
    override fun toItemMeta(itemId: ItemId): ItemMeta {
        val attributes = listOfNotNull(
            ItemMetaAttribute("redeemed", redeemed.toString()),
            taggedTopShot?.let { ItemMetaAttribute("taggedTopShot", it) },
            size?.let { ItemMetaAttribute("size", size) }
        )
        val media = listOfNotNull(video, image)
        return ItemMeta(itemId, title, description, attributes, media)
    }
}

class KicksMetaConverter: JsonCadenceConverter<KicksMeta> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): KicksMeta {
        return com.nftco.flow.sdk.cadence.unmarshall(value) {
            val meta: Map<String, String> = dictionaryMap("metadata") { k, v ->
                val key = string(k)
                if(listOf("video", "image", "size").contains(key)) {
                    key to string(v)
                } else if(key == "redeemed") {
                    key to boolean(v).toString()
                } else key to ""
            }.filterValues { it != "" }

            KicksMeta(
                title = string(compositeValue.getRequiredField("title")),
                description = string(compositeValue.getRequiredField("description")),
                redeemed = meta["redeemed"]?.toBoolean() ?: false,
                size = meta["size"],
                taggedTopShot = meta["taggedTopShot"],
                video = meta["video"],
                image = meta["image"]
            )
        }
    }
}

