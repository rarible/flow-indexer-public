package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.cadence.CadenceNamespace
import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.JsonCadenceConversion
import com.nftco.flow.sdk.cadence.JsonCadenceConverter
import com.nftco.flow.sdk.cadence.StructField
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coFindById
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class MatrixWorldFlowFestMetaProvider(
    @Value("classpath:script/matrix_flow_fest_meta.cdc")
    private val scriptFile: Resource,
    private val itemRepository: ItemRepository,
    private val scriptExecutor: ScriptExecutor
): ItemMetaProvider {

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.contains("Evolution")

    override suspend fun getMeta(itemId: ItemId): ItemMeta {
        val item = itemRepository.coFindById(itemId) ?: return emptyMeta(itemId)
        val meta = scriptExecutor.executeFile(
            scriptFile,
            {
                arg { address(item.owner!!.formatted) }
                arg { uint64(item.tokenId) }
            },
            { json ->
                if(json.value == null) null
                else Flow.unmarshall(MatrixWorldFlowFestNftMeta::class, json.value as StructField)
            }
        ) ?: return emptyMeta(itemId)



        return ItemMeta(
            itemId = itemId,
            name = meta.name,
            description = meta.description,
            attributes = listOf(
                ItemMetaAttribute(key = "type", value = meta.type)
            ),
            contentUrls = listOf(
                meta.animationUrl
            ),
        ).apply {
            raw = toString().toByteArray(Charsets.UTF_8)
        }
    }
}

@JsonCadenceConversion(MatrixWorldFlowFestNftMetaConverter::class)
data class MatrixWorldFlowFestNftMeta(
    val name: String,
    val description: String,
    val animationUrl: String,
    val type: String
)

class MatrixWorldFlowFestNftMetaConverter: JsonCadenceConverter<MatrixWorldFlowFestNftMeta> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): MatrixWorldFlowFestNftMeta {
        return com.nftco.flow.sdk.cadence.unmarshall(value) {
            MatrixWorldFlowFestNftMeta(
                name = string(compositeValue.getRequiredField("name")),
                description = string(compositeValue.getRequiredField("description")),
                animationUrl = string(compositeValue.getRequiredField("animationUrl")),
                type = string(compositeValue.getRequiredField("type"))
            )
        }
    }
}