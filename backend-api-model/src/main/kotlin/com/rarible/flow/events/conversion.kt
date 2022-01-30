package com.rarible.flow.events

import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.cadence.*
import java.math.BigDecimal

// todo after update to flow-jvm-sdk:0.7 remove regexp conversions
private val reg = Regex(""""type":"Capability".*?"address":"([^"]+)","borrowType":"[^"]+"}""")
fun ByteArray.changeCapabilityToAddress() =
    reg.replace(String(this), """"type":"Address","value":"$1"""").toByteArray()

@JsonCadenceConversion(VersusArtItemConverter::class)
data class VersusArtItem(
    val uuid: Long,
    val id: Long,
    val name: String,
    val description: String,
    val schema: String?,
    val contentCapability: String?,
    val contentId: Long?,
    val url: String?,
    val metadata: VersusArtMetadata,
    val royalty: Map<String, VersusArtRoyalty>,
)

class VersusArtItemConverter : JsonCadenceConverter<VersusArtItem> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace) = unmarshall(value) {
        VersusArtItem(
            long("uuid"),
            long("id"),
            string("name"),
            string("description"),
            optional("schema", JsonCadenceParser::string),
            // todo after update to flow-jvm-sdk:0.7
            // optional("contentCapability", JsonCadenceParser::capabilityAddress),
            optional("contentCapability", JsonCadenceParser::address),
            optional("contentId", JsonCadenceParser::long),
            optional("url", JsonCadenceParser::string),
            Flow.unmarshall(VersusArtMetadata::class, compositeValue.getField<StructField>("metadata")!!),
            dictionaryMap("royalty") { k, v -> string(k) to Flow.unmarshall(VersusArtRoyalty::class, v) }
        )
    }
}

@JsonCadenceConversion(VersusArtMetadataConverter::class)
data class VersusArtMetadata(
    val name: String,
    val artist: String,
    val artistAddress: String,
    val description: String,
    val type: String,
    val edition: Long,
    val maxEdition: Long,
)

class VersusArtMetadataConverter : JsonCadenceConverter<VersusArtMetadata> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace) = unmarshall(value) {
        VersusArtMetadata(
            string("name"),
            string("artist"),
            address("artistAddress"),
            string("description"),
            string("type"),
            long("edition"),
            long("maxEdition"),
        )
    }
}

@JsonCadenceConversion(VersusArtRoyaltyConverter::class)
data class VersusArtRoyalty(
    val wallet: String,
    val cut: BigDecimal,
)

class VersusArtRoyaltyConverter : JsonCadenceConverter<VersusArtRoyalty> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace) = unmarshall(value) {
        VersusArtRoyalty(
            // todo after update to flow-jvm-sdk:0.7
            // capabilityAddress("wallet"),
            address("wallet"),
            bigDecimal("cut"),
        )
    }
}
