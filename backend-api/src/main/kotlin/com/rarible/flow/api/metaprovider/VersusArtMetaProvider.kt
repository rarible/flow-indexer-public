package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.JsonCadenceBuilder
import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.events.VersusArtItem
import com.rarible.flow.events.changeCapabilityToAddress
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import java.io.BufferedReader

@Component
class VersusArtMetaProvider(
    private val itemRepository: ItemRepository,
    private val scriptExecutor: ScriptExecutor,
    @Value("classpath:script/versus-art-metadata.cdc")
    private val getMetadataScriptResource: Resource,
    @Value("classpath:script/versus-art-content.cdc")
    private val getContentScriptResource: Resource,
) : ItemMetaProvider {

    private val builder = JsonCadenceBuilder()
    private val parser = JsonCadenceParser()

    private val getMetadataScript = getMetadataScriptResource.readText()
    private val getContentScript = getContentScriptResource.readText()

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.endsWith(".Art")

    override suspend fun getMeta(itemId: ItemId): ItemMeta {
        val item = itemRepository.findById(itemId).awaitSingleOrNull() ?: return emptyMeta(itemId)
        val args: MutableList<Field<*>> = mutableListOf(
            builder.address(item.owner!!.formatted),
            builder.uint64(itemId.tokenId)
        )
        val nft = scriptExecutor
            .execute(code = getMetadataScript, args = args)
            .let { it.copy(bytes = it.bytes.changeCapabilityToAddress()) }
            .let { parser.optional<VersusArtItem>(it.jsonCadence, JsonCadenceParser::unmarshall) }
            ?: return emptyMeta(itemId)

        val content = scriptExecutor
            .execute(code = getContentScript, args = args)
            .let { parser.optional(it.jsonCadence, JsonCadenceParser::string) }

        // valid types: ipfs/image, ipfs/video, png, image/dataurl
        val contentUrl = when (nft.metadata.type) {
            "ipfs/image", "ipfs/video" -> "https://rarible.mypinata.cloud/ipfs/$content"
            else -> content
        }

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

        val urls = listOfNotNull(contentUrl, nft.url)

        return ItemMeta(itemId, nft.name, nft.description, meta, urls)
    }

    private fun Resource.readText() = inputStream.bufferedReader().use(BufferedReader::readText)
}
