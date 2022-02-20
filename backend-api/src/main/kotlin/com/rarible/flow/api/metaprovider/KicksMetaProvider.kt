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
class KicksMetaScript(
    val scriptExecutor: ScriptExecutor,
    @Value("classpath:script/meta/kicks.cdc")
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
class KicksMetaProvider(
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

@JsonCadenceConversion(KicksMetaConverter::class)
data class KicksMeta(
    val title: String,
    val description: String,
    val media: List<String>,
    val redeemed: Boolean,
    val taggedTopShot: String?
): MetaBody {
    override fun toItemMeta(itemId: ItemId): ItemMeta {
        val attributes = listOfNotNull(
            ItemMetaAttribute("redeemed", redeemed.toString()),
            taggedTopShot?.let { ItemMetaAttribute("taggedTopShot", it) }
        )
        return ItemMeta(itemId, title, description, attributes, media)
    }
}

class KicksMetaConverter: JsonCadenceConverter<KicksMeta> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): KicksMeta {
        return com.nftco.flow.sdk.cadence.unmarshall(value) {
            KicksMeta(
                title = string(compositeValue.getRequiredField("title")),
                description = string(compositeValue.getRequiredField("description")),
                redeemed = boolean(compositeValue.getRequiredField("redeemed")),
                media = arrayValues("media") { singleMedia ->
                     string(singleMedia)
                },
                taggedTopShot = optional("taggedTopShot") {
                    string(it)
                }
            )
        }
    }
}

