package com.rarible.flow.api.meta.provider.legacy

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.cadence.OptionalField
import com.rarible.flow.Contracts
import com.rarible.flow.api.meta.ItemMeta
import com.rarible.flow.api.meta.ItemMetaAttribute
import com.rarible.flow.api.meta.ItemMetaContent
import com.rarible.flow.api.meta.provider.ItemMetaProvider
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.TokenId
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class CnnMetaScript(
    private val scriptExecutor: ScriptExecutor,
    @Value("classpath:script/cnn_meta.cdc")
    private val metaScript: Resource
) {
    suspend fun call(setId: Int, edition: Int): CnnNFTMetaBody? {
        return scriptExecutor.executeFile(metaScript, {
            arg { uint32(setId) }
        }, { json ->
            optional(json) { meta ->
                val map = dictionaryMap(meta) { k, v ->
                    string(k) to string(v)
                }
                CnnNFTMetaBody(
                    map["name"]!!,
                    map["description"]!!,
                    map["image"]!!,
                    map["preview"]!!,
                    setId,
                    edition,
                    map["maxEditions"]!!.toInt()
                )
            }
        })
    }
}

@Component
class CnnNftScript(
    private val scriptExecutor: ScriptExecutor,
    @Value("classpath:script/get_cnn_nft.cdc")
    private val cnnNftScript: Resource
) {
    suspend fun call(owner: FlowAddress, tokenId: TokenId): CnnNFT? {
        return scriptExecutor.executeFile(
            cnnNftScript,
            {
                arg { address(owner.formatted) }
                arg { uint64(tokenId) }
            }, {
            CnnNFTConverter.convert(
                it as OptionalField
            )
        }
        )
    }
}

@Component
class CnnNFTMetaProvider(
    private val cnnNftScript: CnnNftScript,
    private val metaScript: CnnMetaScript,
) : ItemMetaProvider {

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.contains(Contracts.CNN.contractName)

    override suspend fun getMeta(item: Item): ItemMeta? {
        return getMeta(
            item,
            this::fetchNft,
            this::readMeta,
        )
    }

    suspend fun fetchNft(item: Item): CnnNFT? {
        return cnnNftScript.call(item.owner ?: item.creator, item.tokenId)
    }

    suspend fun readMeta(cnnNft: CnnNFT): CnnNFTMetaBody? {
        return metaScript.call(cnnNft.setId, cnnNft.editionNum)
    }

    suspend fun getMeta(
        item: Item,
        fetchNft: suspend (Item) -> CnnNFT?,
        fetchIpfsHash: suspend (CnnNFT) -> CnnNFTMetaBody?,
    ): ItemMeta? {
        val cnnNFT = fetchNft(item) ?: return null
        val ipfsMeta = fetchIpfsHash(cnnNFT) ?: return null
        return ipfsMeta.toItemMeta(item.id)
    }
}

data class CnnNFTMetaBody(
    val name: String,
    val description: String,
    val image: String? = null,
    val preview: String? = null,
    val setId: Int,
    val edition: Int,
    val maxEditions: Int
) : MetaBody {

    override fun toItemMeta(itemId: ItemId): ItemMeta {
        return ItemMeta(
            itemId = itemId,
            name = name,
            description = description,
            contentUrls = listOfNotNull(
                image,
                preview
            ),
            attributes = getAttributes(),
            content = listOfNotNull(
                preview?.let {
                    ItemMetaContent(
                        it,
                        ItemMetaContent.Type.IMAGE,
                        ItemMetaContent.Representation.PREVIEW,
                    )
                },
                image?.let {
                    ItemMetaContent(
                        it,
                        ItemMetaContent.Type.IMAGE,
                        ItemMetaContent.Representation.ORIGINAL,
                    )
                }
            )
        ).apply {
            raw = toString().toByteArray(charset = Charsets.UTF_8)
        }
    }

    private fun getAttributes(): List<ItemMetaAttribute> {
        return listOf(
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
