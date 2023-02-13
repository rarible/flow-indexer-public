package com.rarible.flow.scanner.model

import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowEvent
import com.nftco.flow.sdk.FlowScriptResponse
import com.nftco.flow.sdk.cadence.CadenceNamespace
import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.JsonCadenceConversion
import com.nftco.flow.sdk.cadence.JsonCadenceConverter

inline fun <reified T : Any> FlowEvent.parse() = Flow.unmarshall(T::class, event)
inline fun <reified T : Any> FlowScriptResponse.parse() = Flow.unmarshall(T::class, jsonCadence)
inline fun <reified T : Any> Field<*>.parse() = Flow.unmarshall(T::class, this)

@JsonCadenceConversion(MotoGPMetadataConverter::class)
data class MotoGPCardMetadata(
    val cardID: Long,
    val name: String,
    val description: String,
    val imageUrl: String,
    val data: Map<String, String>
)

@JsonCadenceConversion(MotoGPCardNftConverter::class)
data class MotoGPCardNFT(
    val uuid: Long,
    val id: Long,
    val serial: Long,
    val cardID: Long
)

class MotoGPMetadataConverter: JsonCadenceConverter<MotoGPCardMetadata> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): MotoGPCardMetadata =
        com.nftco.flow.sdk.cadence.unmarshall(value.value as Field<*>) {
            MotoGPCardMetadata(
                cardID = long("cardID"),
                name = string("name"),
                description = string("description"),
                imageUrl = string("imageUrl"),
                data = dictionaryMap(field("data")) { key, value ->
                    string(key) to string(value)
                }
            )
        }
}

class MotoGPCardNftConverter: JsonCadenceConverter<MotoGPCardNFT> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): MotoGPCardNFT =
        com.nftco.flow.sdk.cadence.unmarshall(value.value as Field<*>) {
            MotoGPCardNFT(
                uuid = long("uuid"),
                id = long("id"),
                serial = long("serial"),
                cardID = long("cardID")
            )
        }
}

