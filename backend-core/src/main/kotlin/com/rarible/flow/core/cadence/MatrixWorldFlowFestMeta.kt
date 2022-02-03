package com.rarible.flow.core.cadence

import com.nftco.flow.sdk.cadence.CadenceNamespace
import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.JsonCadenceConversion
import com.nftco.flow.sdk.cadence.JsonCadenceConverter


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