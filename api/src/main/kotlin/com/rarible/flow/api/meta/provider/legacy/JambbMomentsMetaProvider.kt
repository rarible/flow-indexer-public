package com.rarible.flow.api.meta.provider.legacy

import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.CadenceNamespace
import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.JsonCadenceConversion
import com.nftco.flow.sdk.cadence.JsonCadenceConverter
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
class JambbMomentsMetaScript(
    private val scriptExecutor: ScriptExecutor,
    @Value("classpath:script/jambb_moment_meta.cdc")
    private val script: Resource
) {
    suspend fun call(tokenId: TokenId): JambbMomentsMeta? {
        return scriptExecutor.executeFile(
            script,
            {
                arg { uint64(tokenId) }
            },
            { json ->
                json as OptionalField
                json.value?.let {
                    Flow.unmarshall(JambbMomentsMeta::class, it)
                }
            }
        )
    }
}

@Component
class JambbMomentsMetaProvider(
    private val script: JambbMomentsMetaScript,
    private val chainId: FlowChainId
) : ItemMetaProvider {
    override fun isSupported(itemId: ItemId): Boolean =
        itemId.contract == Contracts.JAMBB_MOMENTS.fqn(chainId)

    override suspend fun getMeta(item: Item): ItemMeta? {
        return script.call(item.tokenId)?.toItemMeta(item.id)
    }
}

@JsonCadenceConversion(JambbMomentsMetaConverter::class)
data class JambbMomentsMeta(
    val contentCreator: String,
    val contentName: String,
    val contentDescription: String,
    val previewImage: String,
    val videoURI: String,
    val seriesName: String,
    val setName: String,
    val retired: Boolean,
    val rarity: String,
) : MetaBody {
    override fun toItemMeta(itemId: ItemId): ItemMeta {
        return ItemMeta(
            itemId = itemId,
            name = contentName,
            description = contentDescription,
            attributes = listOf(
                ItemMetaAttribute("Creator", contentCreator),
                ItemMetaAttribute("Rarity", rarity),
                ItemMetaAttribute("Retired?", if (retired) "YES" else "NO"),
                ItemMetaAttribute("Set Name", setName),
                ItemMetaAttribute("Series Name", seriesName),
            ),
            contentUrls = listOf(
                previewImage,
                videoURI
            ),
            content = listOf(
                ItemMetaContent(
                    url = previewImage,
                    representation = ItemMetaContent.Representation.PREVIEW,
                    type = ItemMetaContent.Type.IMAGE,
                ),
                ItemMetaContent(
                    url = videoURI,
                    representation = ItemMetaContent.Representation.ORIGINAL,
                    type = ItemMetaContent.Type.VIDEO,
                ),
            )
        )
    }
}

class JambbMomentsMetaConverter : JsonCadenceConverter<JambbMomentsMeta> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): JambbMomentsMeta {
        return com.nftco.flow.sdk.cadence.unmarshall(value) {
            JambbMomentsMeta(
                contentCreator = address("contentCreator"),
                contentName = string("contentName"),
                contentDescription = string("contentDescription"),
                previewImage = string("previewImage"),
                videoURI = string("videoURI"),
                seriesName = string("seriesName"),
                setName = string("setName"),
                retired = boolean("retired"),
                rarity = string("rarity"),
            )
        }
    }
}
