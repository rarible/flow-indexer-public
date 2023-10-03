package com.rarible.flow.core.domain

import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId

@Document(RawOnChainMeta.COLLECTION)
data class RawOnChainMeta(
    @MongoId(FieldType.STRING)
    val id: String,
    val data: String
) {

    companion object {

        const val COLLECTION = "raw_on_chain_meta_cache"
    }
}
