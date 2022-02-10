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
class OneFootballMetaScript(
    val scriptExecutor: ScriptExecutor,
    @Value("classpath:script/one_football_meta.cdc")
    val script: Resource
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
                    Flow.unmarshall(OneFootballMeta::class, it)
                }
            }
        )
    }
}

@Component
class OneFootballMetaProvider(
    val itemRepository: ItemRepository,
    val metaScript: OneFootballMetaScript
): ItemMetaProvider {

    override fun isSupported(itemId: ItemId): Boolean = Contracts.ONE_FOOTBALL.supports(itemId)

    override suspend fun getMeta(itemId: ItemId): ItemMeta {
        return itemRepository
            .coFindById(itemId)
            ?.let { item -> metaScript.call(item.owner ?: item.creator, item.tokenId) }
            ?.toItemMeta(itemId)
            ?: ItemMeta.empty(itemId)
    }
}

@JsonCadenceConversion(OneFootballMetaConverter::class)
data class OneFootballMeta(
    val id: Long,
    val templateID: Long,
    val seriesName: String,
    val name: String,
    val description: String,
    val preview: String,
    val media: String,
    val data: Map<String, String>,
): MetaBody {
    override fun toItemMeta(itemId: ItemId): ItemMeta {
        return ItemMeta(
            itemId,
            name,
            description,
            data.map { (k, v) ->
                ItemMetaAttribute(k, v)
            },
            listOf(
                media,
                preview
            )
        )
    }
}

class OneFootballMetaConverter: JsonCadenceConverter<OneFootballMeta> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): OneFootballMeta {
        return com.nftco.flow.sdk.cadence.unmarshall(value) {
            OneFootballMeta(
                id = long(compositeValue.getRequiredField("id")),
                templateID = long(compositeValue.getRequiredField("templateID")),
                seriesName = string(compositeValue.getRequiredField("seriesName")),
                name = string(compositeValue.getRequiredField("name")),
                description = string(compositeValue.getRequiredField("description")),
                preview = string(compositeValue.getRequiredField("preview")),
                media = string(compositeValue.getRequiredField("media")),
                data = dictionaryMap(compositeValue.getRequiredField("data")) { k, v ->
                    string(k) to string(v)
                }
            )
        }
    }
}

