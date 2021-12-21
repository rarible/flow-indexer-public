package com.rarible.flow.api.metaprovider

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.nftco.flow.sdk.cadence.JsonCadenceBuilder
import com.nftco.flow.sdk.cadence.OptionalField
import com.nftco.flow.sdk.cadence.StringField
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.flow.log.Log
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull

@Component
class CnnNFTMetaProvider(
    private val itemRepository: ItemRepository,
    private val scriptExecutor: ScriptExecutor,
    private val pinataClient: WebClient,

    @Value("classpath:script/get_cnn_nft.cdc")
    private val cnnNftScript: Resource,

    @Value("classpath:script/cnn_meta.cdc")
    private val metaScript: Resource,
) : ItemMetaProvider {

    private val metaScriptText = metaScript.inputStream.bufferedReader().use { it.readText() }
    private val cnnNftScriptText = cnnNftScript.inputStream.bufferedReader().use { it.readText() }

    private val cadenceBuilder = JsonCadenceBuilder()

    private val logger by Log()

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.contains("CNN_NFT")

    override suspend fun getMeta(itemId: ItemId): ItemMeta {
        val item = itemRepository.coFindById(itemId) ?: return emptyMeta(itemId)

        return getMeta(
            item,
            this::fetchNft,
            this::fetchIpfsHash,
            this::readIpfs
        ) { item -> emptyMeta(item.id) }
    }

    suspend fun fetchNft(item: Item): CnnNFT? {
        return CnnNFTConverter.convert(
            scriptExecutor.execute(
                code = cnnNftScriptText,
                args = mutableListOf(
                    cadenceBuilder.address((item.owner ?: item.creator).formatted),
                    cadenceBuilder.uint64(item.tokenId)
                )
            )
        )
    }

    suspend fun fetchIpfsHash(cnnNft: CnnNFT): String? {
        val jsonCadence = scriptExecutor.execute(
            code = metaScriptText,
            args = mutableListOf(
                cadenceBuilder.uint32(cnnNft.setId),
                cadenceBuilder.uint32(cnnNft.editionNum)
            )
        ).jsonCadence as OptionalField

        return if (jsonCadence.value == null) {
            null
        } else (jsonCadence.value as StringField).value!!
    }

    suspend fun readIpfs(ipfsHash: String): CnnNFTMetaBody? {
        return try {
            pinataClient
                .get()
                .uri(ipfsHash)
                .retrieve()
                .awaitBodyOrNull()
        } catch (e: Exception) {
            logger.error("Failed to fetch CNN IPFS by hash [{}]", ipfsHash, e)
            null
        }
    }

    suspend fun getMeta(
        item: Item,
        fetchNft: suspend (Item) -> CnnNFT?,
        fetchIpfsHash: suspend (CnnNFT) -> String?,
        readIpfs: suspend (String) -> CnnNFTMetaBody?,
        defaultValue: (Item) -> ItemMeta
    ): ItemMeta {
        val cnnNFT = fetchNft(item) ?: return defaultValue(item)
        val ipfsHash = fetchIpfsHash(cnnNFT) ?: return defaultValue(item)
        val ipfsMeta = readIpfs(ipfsHash) ?: return defaultValue(item)

        return ItemMeta(
            itemId = item.id,
            name = ipfsMeta.name,
            description = ipfsMeta.description,
            contentUrls = listOfNotNull(
                ipfsMeta.image,
                ipfsMeta.preview,
                ipfsMeta.externalUrl
            ),
            attributes = ipfsMeta.getAttributes()
        ).apply {
            raw = ipfsMeta.toString().toByteArray(charset = Charsets.UTF_8)
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
    val seriesId: Int,

    @get:JsonProperty("set_id")
    val setId: Int,

    @get:JsonProperty("edition")
    val edition: Int,

    @get:JsonProperty("max_editions")
    val maxEditions: Int
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