package com.rarible.flow.api.metaprovider

import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import com.rarible.flow.core.repository.ItemRepository
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class MotoGPItemMetaProvider(
    private val scriptExecutor: ScriptExecutor,
    @Value("classpath:script/motogp-card-metadata.cdc")
    private val scriptFile: Resource,
    private val itemRepository: ItemRepository
) : ItemMetaProvider {

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.contains("MotoGPCard", true)

    override suspend fun getMeta(itemId: ItemId): ItemMeta {
        val item = itemRepository.findById(itemId).awaitSingleOrNull() ?: return emptyMeta(itemId)
        val (nft, meta) = scriptExecutor.executeFile(scriptFile, {
            arg { address(item.owner!!.formatted) }
            arg { uint64(itemId.tokenId) }
        }, { json ->
            array(json) { arr ->
                Pair(
                    optional(arr.value!!.first()) {
                        unmarshall<MotoGPNFT>(it)
                    }!!,
                    optional(arr.value!!.last()) {
                        unmarshall<MotoGPMeta>(it)
                    }!!
                )
            }
        })

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
