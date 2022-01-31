package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.cadence.CadenceNamespace
import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.JsonCadenceConversion
import com.nftco.flow.sdk.cadence.JsonCadenceConverter
import com.nftco.flow.sdk.cadence.OptionalField
import com.rarible.flow.api.metaprovider.body.MetaBody
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coFindById
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class ChainmonstersMetaProvider(
    private val itemRepository: ItemRepository,
    private val scriptExecutor: ScriptExecutor,
    @Value("classpath:script/chainmonsters_meta.cdc")
    private val script: Resource
): ItemMetaProvider {

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.contains("ChainmonstersRewards")

    override suspend fun getMeta(itemId: ItemId): ItemMeta {
        return itemRepository
            .coFindById(itemId)
            ?.let { item ->
                scriptExecutor.executeFile(
                    script,
                    {
                        arg { address(item.owner!!.formatted) }
                        arg { uint64(itemId.tokenId) }
                    },
                    { response ->
                        response as OptionalField
                        response.value?.let {
                            Flow.unmarshall(ChainmonstersMeta::class, it)
                        }
                    }
                )
            }?.toItemMeta(itemId) ?: ItemMeta.empty(itemId)
    }
}

@JsonCadenceConversion(ChainmonstersMetaConversion::class)
data class ChainmonstersMeta(
    val rewardId: Int,
    val title: String?,
): MetaBody {
    override fun toItemMeta(itemId: ItemId): ItemMeta {
        return ItemMeta(
            itemId,
            title ?: "",
            "",
            emptyList(),
            listOf(
                "https://chainmonsters.com/images/rewards/flowfest2021/${rewardId}.png"
            )
        )
    }
}

class ChainmonstersMetaConversion: JsonCadenceConverter<ChainmonstersMeta> {

    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): ChainmonstersMeta {
        return com.nftco.flow.sdk.cadence.unmarshall(value) {
            ChainmonstersMeta(
                rewardId = int(compositeValue.getRequiredField("rewardId")),
                title = optional(compositeValue.getRequiredField("title")) {
                    string(it)
                }
            )
        }
    }
}
