package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.cadence.CadenceNamespace
import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.JsonCadenceConversion
import com.nftco.flow.sdk.cadence.JsonCadenceConverter
import com.nftco.flow.sdk.cadence.OptionalField
import com.rarible.flow.Contracts
import com.rarible.flow.api.config.ApiProperties
import com.rarible.flow.api.metaprovider.body.MetaBody
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.withEntity
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class JambbMomentsMetaScript(
    private val scriptExecutor: ScriptExecutor,
    @Value("classpath:script/meta_jambb_moment.cdc")
    private val script: Resource
) {
    suspend fun call(tokenId: TokenId): MetaBody? {
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
    private val itemRepository: ItemRepository,
    private val script: JambbMomentsMetaScript,
    private val apiProperties: ApiProperties
): ItemMetaProvider {
    override fun isSupported(itemId: ItemId): Boolean =
        itemId.contract == Contracts.JAMBB_MOMENTS.fqn(apiProperties.chainId)

    override suspend fun getMeta(itemId: ItemId): ItemMeta {
        return itemRepository.withEntity(itemId) { item ->
            script.call(item.tokenId)
        }?.toItemMeta(itemId) ?: ItemMeta.empty(itemId)
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
): MetaBody {
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
                ItemMeta.Content(
                    url = previewImage,
                    representation = ItemMeta.Content.Representation.PREVIEW,
                    type = ItemMeta.Content.Type.IMAGE,
                ),
                ItemMeta.Content(
                    url = videoURI,
                    representation = ItemMeta.Content.Representation.ORIGINAL,
                    type = ItemMeta.Content.Type.VIDEO,
                ),
            )
        )
    }
}

class JambbMomentsMetaConverter: JsonCadenceConverter<JambbMomentsMeta> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): JambbMomentsMeta {
        return com.nftco.flow.sdk.cadence.unmarshall(value) {
            JambbMomentsMeta(
                contentCreator = address(compositeValue.getRequiredField("contentCreator")),
                contentName = string(compositeValue.getRequiredField("contentName")),
                contentDescription = string(compositeValue.getRequiredField("contentDescription")),
                previewImage = string(compositeValue.getRequiredField("previewImage")),
                videoURI = string(compositeValue.getRequiredField("videoURI")),
                seriesName = string(compositeValue.getRequiredField("seriesName")),
                setName = string(compositeValue.getRequiredField("setName")),
                retired = boolean(compositeValue.getRequiredField("retired")),
                rarity = string(compositeValue.getRequiredField("rarity"))
            )
        }
    }
}