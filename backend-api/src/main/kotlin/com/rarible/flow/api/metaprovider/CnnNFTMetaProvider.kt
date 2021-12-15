package com.rarible.flow.api.metaprovider

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.cadence.JsonCadenceBuilder
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.log.Log
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.json.JacksonJsonParser
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull

@Component
class CnnNFTMetaProvider(
    private val itemRepository: ItemRepository,
    private val scriptExecutor: ScriptExecutor,
    @Value("classpath:script/cnn_meta.cdc")
    private val metaScript: Resource,

    @Value("classpath:script/get_cnn_nft.cdc")
    private val cnnNftScript: Resource,
) : ItemMetaProvider {

    private val metaScriptText = metaScript.inputStream.bufferedReader().use { it.readText() }
    private val cnnNftScriptText = cnnNftScript.inputStream.bufferedReader().use { it.readText() }

    private val cadenceBuilder = JsonCadenceBuilder()

    private val logger by Log()

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.contains("CNN_NFT")

    override suspend fun getMeta(itemId: ItemId): ItemMeta {
        val item = itemRepository.coFindById(itemId) ?: return emptyMeta(itemId)

        val cnnNFT: CnnNFT = scriptExecutor.execute(
            code = cnnNftScriptText,
            args = mutableListOf(
                cadenceBuilder.address((item.owner ?: item.creator).formatted),
                cadenceBuilder.uint64(item.tokenId)
            )
        ).jsonCadence.let { Flow.unmarshall(CnnNFT::class, it) }


        val ipfsHash = scriptExecutor.execute(
            code = metaScriptText,
            args = mutableListOf(
                cadenceBuilder.uint32(cnnNFT.setId),
                cadenceBuilder.uint32(cnnNFT.editionNum)
            )
        )


        val url = "https://rarible.mypinata.cloud/ipfs/${ipfsHash}"

        val client = WebClient.create()
        return try {
            val data = client.get().uri(url)
                .retrieve().awaitBodyOrNull<CnnNFTMetaBody>() ?: return emptyMeta(itemId)
            ItemMeta(
                itemId = itemId,
                name = data.name,
                description = data.description,
                contentUrls = listOfNotNull(
                    data.image,
                    data.preview,
                    data.externalUrl
                ),
                attributes = data.getAttributes()
            ).apply {
                raw = data.toString().toByteArray(charset = Charsets.UTF_8)
            }
        } catch (e: Exception) {
            logger.warn(e.message, e)
            return emptyMeta(itemId)
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class CnnNFTMetaBody(
    val name: String,
    val description: String,
    val image: String? = null,
    val preview: String? = null,

    @get:JsonProperty("external_url")
    val externalUrl: String? = null,

    @get:JsonProperty("creator_name")
    val creatorName: String,

    val seriesName: String,
    val seriesDescription: String,

    @get:JsonProperty("series_id")
    val seriesId: Integer,

    @get:JsonProperty("set_id")
    val setId: Integer,

    @get:JsonProperty("edition")
    val edition: Integer,

    @get:JsonProperty("max_editions")
    val maxEditions: Integer
) {
    fun getAttributes(): List<ItemMetaAttribute> {
        return listOf(
            ItemMetaAttribute(
                key = "seriesName",
                value = seriesName
            ),
            ItemMetaAttribute(
                key = "seriesDescription",
                value = seriesDescription
            ),
            ItemMetaAttribute(
                key = "seriesId",
                value = seriesId.toString()
            ),
            ItemMetaAttribute(
                key = "setId",
                value = setId.toString()
            ),
            ItemMetaAttribute(
                key = "edition",
                value = edition.toString()
            ),
            ItemMetaAttribute(
                key = "maxEditions",
                value = maxEditions.toString()
            )
        )
    }
}