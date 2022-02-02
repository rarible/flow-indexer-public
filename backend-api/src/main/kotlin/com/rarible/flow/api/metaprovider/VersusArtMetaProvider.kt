package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.cadence.JsonCadenceBuilder
import com.nftco.flow.sdk.cadence.ResourceField
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import com.rarible.flow.events.VersusArtItem
import com.rarible.flow.events.changeCapabilityToAddress
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class VersusArtMetaProvider(
    private val scriptExecutor: ScriptExecutor,
    @Value("classpath:script/versus-art-metadata.cdc")
    private val scriptFile: Resource,
) : ItemMetaProvider {

    private val builder = JsonCadenceBuilder()

    private lateinit var scriptText: String

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.contains("VersusArt")

    override suspend fun getMeta(item: Item): ItemMeta? {
        val itemId = item.id
        val nft = scriptExecutor.executeFile(
            scriptFile,
            {
                arg { address(item.owner!!.formatted) }
                arg { ufix64(itemId.tokenId) }
            },
            {
                (it.value as ResourceField?)?.let { it1 -> Flow.unmarshall(VersusArtItem::class, it1) }
            },
            {
                this.copy(bytes = this.bytes.changeCapabilityToAddress())
            }
        ) ?: return null
        val meta = listOf(
            ItemMetaAttribute("uuid", "${nft.uuid}"),
            ItemMetaAttribute("id", "${nft.id}"),
            ItemMetaAttribute("name", nft.metadata.name),
            ItemMetaAttribute("artist", nft.metadata.artist),
            ItemMetaAttribute("artistAddress", nft.metadata.artistAddress),
            ItemMetaAttribute("description", nft.metadata.description),
            ItemMetaAttribute("type", nft.metadata.type),
            ItemMetaAttribute("edition", nft.metadata.edition.toString()),
            ItemMetaAttribute("maxEdition", nft.metadata.maxEdition.toString()),
            ItemMetaAttribute("contentId", nft.contentId.toString()),
            ItemMetaAttribute("schema", nft.schema.toString()),
        )
        val urls = listOfNotNull(nft.url)

        return ItemMeta(itemId, nft.name, nft.description, meta, urls)
    }

    @PostConstruct
    private fun readScript() {
        scriptText = scriptFile.inputStream.bufferedReader().use { it.readText() }
    }
}
