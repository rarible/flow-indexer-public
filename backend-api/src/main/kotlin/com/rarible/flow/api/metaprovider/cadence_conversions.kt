package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.cadence.CadenceNamespace
import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.JsonCadenceConversion
import com.nftco.flow.sdk.cadence.JsonCadenceConverter

@JsonCadenceConversion(MotoGPNFTConverter::class)
data class MotoGPNFT(
    val uuid: Long,
    val id: Long,
    val cardID: Long,
    val serial: Int
)

class MotoGPNFTConverter: JsonCadenceConverter<MotoGPNFT> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): MotoGPNFT = com.nftco.flow.sdk.cadence.unmarshall(value) {
        MotoGPNFT(
            uuid = long(compositeValue.getRequiredField("uuid")),
            id = long(compositeValue.getRequiredField("id")),
            cardID = long(compositeValue.getRequiredField("cardID")),
            serial = int(compositeValue.getRequiredField("serial"))
        )
    }
}

@JsonCadenceConversion(MotoGPMetaConverter::class)
data class MotoGPMeta(
    val cardID: Long,
    val name: String,
    val description: String,
    val imageUrl: String,
    val data: Map<String, String>
)


class MotoGPMetaConverter: JsonCadenceConverter<MotoGPMeta> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): MotoGPMeta = com.nftco.flow.sdk.cadence.unmarshall(value) {
        MotoGPMeta(
            cardID = long(compositeValue.getRequiredField("cardID")),
            name = string(compositeValue.getRequiredField("name")),
            description = string(compositeValue.getRequiredField("description")),
            imageUrl = string(compositeValue.getRequiredField("imageUrl")),
            data = dictionaryMap(compositeValue.getRequiredField("data")) { k, v -> string(k) to string(v) }
        )
    }
}
