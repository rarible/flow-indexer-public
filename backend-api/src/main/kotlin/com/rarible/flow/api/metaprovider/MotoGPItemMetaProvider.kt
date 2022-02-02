package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import com.rarible.flow.core.domain.TokenId
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class MotoGPItemMetaProvider(
    private val motoGpCardScript: MotoGpCardScript
) : ItemMetaProvider {

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.contains("MotoGPCard", true)

    override suspend fun getMeta(item: Item): ItemMeta? {
        return motoGpCardScript(item.owner!!, item.tokenId).toItemMeta(item.id)
    }
}

@Component
class MotoGpCardScript(
    private val scriptExecutor: ScriptExecutor,
    @Value("classpath:script/motogp-card-metadata.cdc")
    private val scriptFile: Resource
) {
    suspend operator fun invoke(owner: FlowAddress, tokenId: TokenId): MotoGpMetaBody {
        return scriptExecutor.executeFile(scriptFile, {
            arg { address(owner.formatted) }
            arg { uint64(tokenId) }
        }, { json ->
            array(json) { arr ->
                val value = arr.value!!
                MotoGpMetaBody(
                    optional(value.first()) { nft ->
                        unmarshall(nft)
                    }!!,
                    optional(value.last()) { meta ->
                        unmarshall(meta)
                    }!!
                )
            }
        })
    }
}

data class MotoGpMetaBody(
    val nft: MotoGPNFT,
    val meta: MotoGPMeta
): MetaBody {
    override fun toItemMeta(itemId: ItemId): ItemMeta {
        val attributes = meta.data.filterNot { "videoUrl" == it.key }.map { e ->
            ItemMetaAttribute(
                key = e.key,
                value = e.value,
            )
        }.toMutableList()
        attributes.add(ItemMetaAttribute(key = "uuid", value = "${nft.uuid}"))
        attributes.add(ItemMetaAttribute(key = "id", value = "${nft.id}"))
        attributes.add(ItemMetaAttribute(key = "cardID", value = "${nft.cardID}"))
        attributes.add(ItemMetaAttribute(key = "serial", value = "${nft.serial}"))
        return ItemMeta(
            itemId = itemId,
            name = meta.name,
            description = meta.description,
            attributes = attributes.toList(),
            contentUrls = listOf(meta.imageUrl, meta.data["videoUrl"].orEmpty())
        ).apply {
            raw = this.toString().toByteArray(charset = Charsets.UTF_8)
        }
    }
}
