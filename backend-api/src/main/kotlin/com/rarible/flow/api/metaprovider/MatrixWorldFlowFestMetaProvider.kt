package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.cadence.CadenceNamespace
import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.JsonCadenceBuilder
import com.nftco.flow.sdk.cadence.JsonCadenceConversion
import com.nftco.flow.sdk.cadence.JsonCadenceConverter
import com.nftco.flow.sdk.cadence.OptionalField
import com.nftco.flow.sdk.cadence.StructField
import com.rarible.flow.Contracts
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

    private val cadenceBuilder = JsonCadenceBuilder()

    private val scriptText: String by lazy {
        scriptFile.inputStream.bufferedReader().use { it.readText() }
    }

    override fun isSupported(itemId: ItemId): Boolean = Contracts.MATRIX_WORLD_FLOW_FEST.supports(itemId)

    override suspend fun getMeta(itemId: ItemId): ItemMeta {
        val item = itemRepository.coFindById(itemId) ?: return emptyMeta(itemId)
        val resp = scriptExecutor.execute(
            code = scriptText,
            args = mutableListOf(
                cadenceBuilder.address(item.owner!!.formatted),
                cadenceBuilder.uint64(item.tokenId)
            )
        ).jsonCadence as OptionalField

        if(resp.value == null) return emptyMeta(itemId)

        val meta = Flow.unmarshall(MatrixWorldFlowFestNftMeta::class, resp.value as StructField)

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