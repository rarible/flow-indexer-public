package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.JsonCadenceBuilder
import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import com.rarible.flow.core.event.VersusArtItem
import com.rarible.flow.core.event.changeCapabilityToAddress
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import java.io.BufferedReader

@Component
class VersusArtMetaProvider(
    private val scriptExecutor: ScriptExecutor,
    @Value("classpath:script/versus-art-metadata.cdc")
    private val getMetadataScriptResource: Resource,
    @Value("classpath:script/versus-art-content.cdc")
    private val getContentScriptResource: Resource,
    @Value("\${app.web-api-url}")
    private val webApiUrl: String
) : ItemMetaProvider {

    private val logger: Logger = LoggerFactory.getLogger(VersusArtMetaProvider::class.java)

    private val builder = JsonCadenceBuilder()
    private val parser = JsonCadenceParser()

    private val getMetadataScript = getMetadataScriptResource.readText()
    private val getContentScript = getContentScriptResource.readText()

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.endsWith(".Art")

    override suspend fun getMeta(item: Item): ItemMeta? {
        val args: MutableList<Field<*>> = mutableListOf(
            builder.address(item.owner!!.formatted),
            builder.uint64(item.tokenId)
        )
        val nft = scriptExecutor
            .execute(code = getMetadataScript, args = args)
            .let { it.copy(bytes = it.bytes.changeCapabilityToAddress()) }
            .let { parser.optional<VersusArtItem>(it.jsonCadence, JsonCadenceParser::unmarshall) }
            ?: return null

        val content = kotlin.runCatching {
            scriptExecutor
                .execute(code = getContentScript, args = args)
                .let { parser.optional(it.jsonCadence, JsonCadenceParser::string) }!!
        }.onFailure {
            logger.error("Can't get content for $${item.id}", it)
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

        var base64: String? = null
        // valid types: ipfs/image, ipfs/video, png, image/dataurl
        val contentUrl = content.getOrNull()?.let {
            when (nft.metadata.type) {
                "ipfs/image", "ipfs/video" -> "ipfs://ipfs/$it"
                else -> {
                    base64 = it
                    "${webApiUrl}/v0.1/items/${item.id}/image"
                }
            }
        }

        val urls = listOfNotNull(contentUrl, nft.url)

        return ItemMeta(
            item.id,
            nft.name,
            nft.description,
            meta,
            urls,
            content = listOfNotNull(
                (base64 ?: contentUrl)?.let {
                    ItemMeta.Content(
                        url = it,
                        representation = ItemMeta.Content.Representation.ORIGINAL,
                        type = ItemMeta.Content.Type.IMAGE,
                    )
                },
                nft.url?.let {
                    ItemMeta.Content(
                        url = it,
                        representation = ItemMeta.Content.Representation.PREVIEW,
                        type = ItemMeta.Content.Type.IMAGE,
                    )
                },
            )
        ).apply {
            this.base64 = base64
        }
    }

    private fun Resource.readText() = inputStream.bufferedReader().use(BufferedReader::readText)
}
