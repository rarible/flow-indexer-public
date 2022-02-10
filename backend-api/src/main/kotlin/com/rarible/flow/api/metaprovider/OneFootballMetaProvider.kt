package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.cadence.OptionalField
import com.rarible.flow.Contracts
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.log.Log
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class OneFootballMetaProvider(
    val itemRepository: ItemRepository,
    val scriptExecutor: ScriptExecutor,
    @Value("classpath:script/one_football_meta.cdc")
    val script: Resource
): ItemMetaProvider {

    private val logger by Log()

    override fun isSupported(itemId: ItemId): Boolean = Contracts.ONE_FOOTBALL.supports(itemId)

    override suspend fun getMeta(item: Item): ItemMeta? {
        return scriptExecutor.executeFile(
                    script,
                    {
                        arg { address(item.owner!!.formatted) }
                        arg { uint64(item.tokenId) }
                    },
                    { response ->
                        response as OptionalField
                        response.value?.let {
                            Flow.unmarshall(OneFootballMeta::class, it)
                        }
                    }
                )?.toItemMeta(item.id)

    }
}

data class OneFootballMeta(
    val id: Long,
    val templateID: Long,
    val seriesName: String,
    val name: String,
    val description: String,
    val preview: String,
    val media: String,
    val data: Map<String, String>,
    val url: String,
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

