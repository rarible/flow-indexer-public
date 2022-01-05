package com.rarible.flow.api.metaprovider

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.nftco.flow.sdk.cadence.OptionalField
import com.nftco.flow.sdk.cadence.StringField
import com.rarible.flow.api.metaprovider.body.MetaBody
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import com.rarible.flow.core.repository.ItemRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Component
class CnnNFTMetaProvider(
    private val scriptExecutor: ScriptExecutor,
    private val pinataClient: WebClient,

    @Value("classpath:script/get_cnn_nft.cdc")
    private val cnnNftScript: Resource,

    @Value("classpath:script/cnn_meta.cdc")
    private val metaScript: Resource,
) : ItemMetaProvider {

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.contains("CNN_NFT")

    override suspend fun getMeta(item: Item): ItemMeta? {
        return getMeta(
            item,
            this::fetchNft,
            this::fetchIpfsHash,
            this::readIpfs
        )
    }

    suspend fun fetchNft(item: Item): CnnNFT? {
        return scriptExecutor.executeFile(
            cnnNftScript,
            {
                arg { address((item.owner ?: item.creator).formatted) }
                arg { uint64(item.tokenId) }
            }, {
                CnnNFTConverter.convert(
                    it as OptionalField
                )
            }
        )

    }

    suspend fun fetchIpfsHash(cnnNft: CnnNFT): String? {
        return scriptExecutor.executeFile(metaScript, {
            arg { uint32(cnnNft.setId) }
            arg { uint32(cnnNft.editionNum) }
        }, { json ->
            json as OptionalField
            if (json.value == null) {
                null
            } else (json.value as StringField).value!!
        })
    }

    suspend fun readIpfs(ipfsHash: String): CnnNFTMetaBody {
        return pinataClient
            .get()
            .uri("/ipfs/$ipfsHash")
            .retrieve()
            .awaitBody()
    }

    suspend fun getMeta(
        item: Item,
        fetchNft: suspend (Item) -> CnnNFT?,
        fetchIpfsHash: suspend (CnnNFT) -> String?,
        readIpfs: suspend (String) -> CnnNFTMetaBody
    ): ItemMeta? {
        val cnnNFT = fetchNft(item) ?: return null
        val ipfsHash = fetchIpfsHash(cnnNFT) ?: return null
        val ipfsMeta = readIpfs(ipfsHash)
        return ipfsMeta.toItemMeta(item.id)
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
) : MetaBody {

    override fun toItemMeta(itemId: ItemId): ItemMeta {
        return ItemMeta(
            itemId = itemId,
            name = name,
            description = description,
            contentUrls = listOfNotNull(
                image,
                preview,
                externalUrl
            ),
            attributes = getAttributes()
        ).apply {
            raw = toString().toByteArray(charset = Charsets.UTF_8)
        }
    }

    private fun getAttributes(): List<ItemMetaAttribute> {
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