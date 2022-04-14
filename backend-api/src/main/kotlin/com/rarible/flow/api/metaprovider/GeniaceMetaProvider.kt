package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.cadence.CadenceNamespace
import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.JsonCadenceConversion
import com.nftco.flow.sdk.cadence.JsonCadenceConverter
import com.nftco.flow.sdk.cadence.unmarshall
import com.rarible.flow.Contracts
import com.rarible.flow.api.metaprovider.body.MetaBody
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coFindById
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class GeniaceMetaProvider(
    val itemRepository: ItemRepository,
    val getMetaScript: GeniaceGetMetaScript,
) : ItemMetaProvider {
    override fun isSupported(itemId: ItemId): Boolean = Contracts.GENIACE.supports(itemId)

    override suspend fun getMeta(itemId: ItemId) = itemRepository
        .coFindById(itemId)
        ?.let { getMeta(it) }
        ?: ItemMeta.empty(itemId)

    private suspend fun getMeta(item: Item): ItemMeta? {
        return getMetaScript(item.owner ?: item.creator, item.tokenId)
            ?.toItemMeta(item.id)
    }
}

@Component
class GeniaceGetMetaScript(
    private val scriptExecutor: ScriptExecutor,
    @Value("classpath:script/meta/geniace_meta.cdc")
    private val script: Resource,
) {
    suspend operator fun invoke(owner: FlowAddress, tokenId: TokenId) = scriptExecutor.executeFile(
        script,
        {
            arg { address(owner.formatted) }
            arg { uint64(tokenId) }
        },
        { result ->
            optional(result) {
                unmarshall(it, GeniaceNFT::class)
            }
        }
    )
}

@JsonCadenceConversion(GeniaceNFTConverter::class)
data class GeniaceNFT(
    val id: Long,
    val metadata: GeniaceMetadata,
) : MetaBody {
    override fun toItemMeta(itemId: ItemId) = ItemMeta(
        itemId,
        metadata.name,
        metadata.description,
        attributes(),
        media(),
    )

    private fun media() = listOf(metadata.imageUrl) +
            metadata.data.filterKeys { it.startsWith("gallery") }.entries
                .sortedBy { (key, _) -> key }
                .map { (_, value) -> value }

    private fun attributes() = listOf(
        ItemMetaAttribute("celebrityName", metadata.celebrityName),
        ItemMetaAttribute("artist", metadata.artist),
        ItemMetaAttribute("rarity", metadata.rarity.name),
    ) + metadata.data
        .filterKeys { !it.startsWith("gallery") && it != "mimetype" }
        .map { (key, value) -> ItemMetaAttribute(key, value) }
}

@JsonCadenceConversion(GeniaceMetadataConverter::class)
data class GeniaceMetadata(
    val name: String,
    val description: String,
    val imageUrl: String,
    val celebrityName: String,
    val artist: String,
    val rarity: GeniaceRarity,
    val data: Map<String, String>,
)

class GeniaceNFTConverter : JsonCadenceConverter<GeniaceNFT> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace) = unmarshall(value) {
        GeniaceNFT(
            long("id"),
            unmarshall("metadata", GeniaceMetadata::class),
        )
    }
}

enum class GeniaceRarity { Collectible, Rare, UltraRare }

class GeniaceMetadataConverter : JsonCadenceConverter<GeniaceMetadata> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace) = unmarshall(value) {
        GeniaceMetadata(
            string("name"),
            string("description"),
            string("imageUrl"),
            string("celebrityName"),
            string("artist"),
            enum("rarity"),
            dictionaryMap("data") { key, value ->
                string(key) to string(value)
            }
        )
    }
}
