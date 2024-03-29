package com.rarible.flow.api.meta.provider.legacy

import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.cadence.CadenceNamespace
import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.JsonCadenceConversion
import com.nftco.flow.sdk.cadence.JsonCadenceConverter
import com.nftco.flow.sdk.cadence.OptionalField
import com.nftco.flow.sdk.cadence.unmarshall
import com.rarible.flow.core.domain.Part

@JsonCadenceConversion(MotoGPNFTConverter::class)
data class MotoGPNFT(
    val uuid: Long,
    val id: Long,
    val cardID: Long,
    val serial: Int
)

class MotoGPNFTConverter : JsonCadenceConverter<MotoGPNFT> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): MotoGPNFT =
        com.nftco.flow.sdk.cadence.unmarshall(value) {
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

class MotoGPMetaConverter : JsonCadenceConverter<MotoGPMeta> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): MotoGPMeta =
        com.nftco.flow.sdk.cadence.unmarshall(value) {
            MotoGPMeta(
                cardID = long(compositeValue.getRequiredField("cardID")),
                name = string(compositeValue.getRequiredField("name")),
                description = string(compositeValue.getRequiredField("description")),
                imageUrl = string(compositeValue.getRequiredField("imageUrl")),
                data = dictionaryMap(compositeValue.getRequiredField("data")) { k, v -> string(k) to string(v) }
            )
        }
}

@JsonCadenceConversion(RaribleNFTConverter::class)
data class RaribleNFT(
    val uuid: Long,
    val creator: FlowAddress,
    val metadata: Map<String, String>,
    val royalties: List<Part>
)

class RaribleNFTConverter : JsonCadenceConverter<RaribleNFT> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): RaribleNFT =
        com.nftco.flow.sdk.cadence.unmarshall(value) {
            RaribleNFT(
                uuid = long(compositeValue.getRequiredField("uuid")),
                creator = FlowAddress(address(compositeValue.getRequiredField("creator"))),
                metadata = dictionaryMap(compositeValue.getRequiredField("metadata")) { k, v -> string(k) to string(v) },
                royalties = arrayValues(compositeValue.getRequiredField("royalties")) {
                    unmarshall(it) {
                        Part(
                            address = FlowAddress(address(compositeValue.getRequiredField("address"))),
                            fee = double(compositeValue.getRequiredField("fee"))
                        )
                    }
                }
            )
        }
}

@JsonCadenceConversion(MugenNFTConverter::class)
data class MugenNFT(
    val uuid: Long,
    val id: Long,
    val typeId: Long,
)

class MugenNFTConverter : JsonCadenceConverter<MugenNFT> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): MugenNFT =
        com.nftco.flow.sdk.cadence.unmarshall(value) {
            MugenNFT(
                uuid = long(compositeValue.getRequiredField("uuid")),
                id = long(compositeValue.getRequiredField("id")),
                typeId = long(compositeValue.getRequiredField("typeID")),
            )
        }
}

@JsonCadenceConversion(CnnNFTConverter::class)
data class CnnNFT(
    val id: Long,
    val setId: Int,
    val editionNum: Int
)

class CnnNFTConverter : JsonCadenceConverter<CnnNFT> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): CnnNFT =
        com.nftco.flow.sdk.cadence.unmarshall(value) {
            CnnNFT(
                id = long(compositeValue.getRequiredField("id")),
                setId = int(compositeValue.getRequiredField("setId")),
                editionNum = int(compositeValue.getRequiredField("editionNum")),
            )
        }

    companion object {
        fun convert(jsonOptional: OptionalField): CnnNFT? {
            val value = jsonOptional.value
            return when (value) {
                null -> null
                is OptionalField -> convert(value)
                else -> Flow.unmarshall(CnnNFT::class, value)
            }
        }
    }
}

@JsonCadenceConversion(DisruptArtNFTConversion::class)
data class DisruptArtNFT(
    val id: Long,
    val name: String,
    val creator: String,
    val metaData: Map<String, String>
)

class DisruptArtNFTConversion : JsonCadenceConverter<DisruptArtNFT> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): DisruptArtNFT = com.nftco.flow.sdk.cadence.unmarshall(value) {
        DisruptArtNFT(
            id = long(compositeValue.getRequiredField("id")),
            name = string(compositeValue.getRequiredField("name")),
            creator = optional(compositeValue.getRequiredField("creator")) {
                address(it)
            }!!,
            metaData = dictionaryMap(compositeValue.getRequiredField("metaData")) { k, v ->
                string(k) to string(v)
            }
        )
    }
}
