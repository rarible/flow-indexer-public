package com.rarible.flow.core.event

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.cadence.CadenceNamespace
import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.JsonCadenceConversion
import com.nftco.flow.sdk.cadence.JsonCadenceConverter
import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.nftco.flow.sdk.cadence.StructField
import com.nftco.flow.sdk.cadence.marshall
import com.nftco.flow.sdk.cadence.unmarshall
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

@JsonCadenceConversion(RaribleNFTv2TokenConverter::class)
data class RaribleNFTv2Token(
    val id: Long,
    val parentId: Long,
    val creator: String,
    val meta: RaribleNFTv2Meta,
    val royalties: List<RaribleNFTv2Royalty>,
)

@JsonCadenceConversion(RaribleNFTv2MetaConverter::class)
data class RaribleNFTv2Meta(
    val name: String,
    val description: String?,
    val cid: String,
    val attributes: Map<String, String>,
    val contentUrls: List<String>,
) {
    fun toMap(): Map<String, String> = mapOf(
        "name" to name,
        "description" to description.orEmpty(),
        "cid" to cid,
        "attributes" to jacksonObjectMapper().writeValueAsString(attributes),
        "contentUrls" to jacksonObjectMapper().writeValueAsString(contentUrls)
    )
}

@JsonCadenceConversion(RaribleNFTv2RoyaltyConverter::class)
data class RaribleNFTv2Royalty(
    val address: String,
    val fee: BigDecimal,
)

class RaribleNFTv2RoyaltyConverter : JsonCadenceConverter<RaribleNFTv2Royalty> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): RaribleNFTv2Royalty = unmarshall(value) {
        RaribleNFTv2Royalty(address("address"), bigDecimal("fee"))
    }

    override fun marshall(value: RaribleNFTv2Royalty, namespace: CadenceNamespace): Field<*> =
        marshall {
            struct {
                compositeOfPairs(namespace.withNamespace("RaribleNFTv2.Royalty")) {
                    listOf(
                        "address" to address(value.address),
                        "fee" to ufix64(value.fee),
                    )
                }
            }
        }
}

class RaribleNFTv2TokenConverter : JsonCadenceConverter<RaribleNFTv2Token> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): RaribleNFTv2Token = unmarshall(value) {
        RaribleNFTv2Token(
            id = long("id"),
            parentId = long("parentId"),
            meta = unmarshall("meta"),
            creator = address("creator"),
            royalties = arrayValues("royalties", JsonCadenceParser::unmarshall),
        )
    }
}

class RaribleNFTv2MetaConverter : JsonCadenceConverter<RaribleNFTv2Meta> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): RaribleNFTv2Meta = unmarshall(value) {
        RaribleNFTv2Meta(
            name = string("name"),
            description = optional("description", JsonCadenceParser::string),
            cid = string("cid"),
            attributes = dictionaryMap("attributes") { key, value -> string(key) to string(value) },
            contentUrls = arrayValues("contentUrls", JsonCadenceParser::string),
        )
    }

    override fun marshall(value: RaribleNFTv2Meta, namespace: CadenceNamespace): Field<*> =
        marshall {
            struct {
                compositeOfPairs(namespace.withNamespace("RaribleNFTv2.Meta")) {
                    listOf(
                        "name" to string(value.name),
                        "description" to optional(value.description?.let(::string)),
                        "cid" to string(value.cid),
                        "attributes" to dictionaryOfMap {
                            value.attributes.map { (key, value) ->
                                string(key) to string(value)
                            }.toMap()
                        },
                        "contentUrls" to array { value.contentUrls.map(::string) }
                    )
                }
            }
        }
}
