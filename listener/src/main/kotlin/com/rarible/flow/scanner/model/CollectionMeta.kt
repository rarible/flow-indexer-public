package com.rarible.flow.scanner.model

import com.nftco.flow.sdk.cadence.CadenceNamespace
import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.JsonCadenceConversion
import com.nftco.flow.sdk.cadence.JsonCadenceConverter
import com.nftco.flow.sdk.cadence.JsonCadenceParser

@JsonCadenceConversion(CollectionMetaConversion::class)
data class CollectionMeta(
    val name: String,
    val symbol: String,
    val icon: String?,
    val description: String?,
    val url: String?
)

class CollectionMetaConversion : JsonCadenceConverter<CollectionMeta> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): CollectionMeta =
        com.nftco.flow.sdk.cadence.unmarshall(value) {
            CollectionMeta(
                name = string("name"),
                symbol = string("symbol"),
                description = optional("description", JsonCadenceParser::string),
                icon = optional("icon", JsonCadenceParser::string),
                url = optional("url", JsonCadenceParser::string),
            )
        }
}
