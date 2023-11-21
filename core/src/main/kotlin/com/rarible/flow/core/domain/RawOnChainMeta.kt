package com.rarible.flow.core.domain

import com.rarible.core.common.nowMillis
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId
import java.time.Instant

@Document(RawOnChainMeta.COLLECTION)
data class RawOnChainMeta(
    @MongoId(FieldType.STRING)
    val id: String,
    val data: String,
    @Field(targetType = FieldType.DATE_TIME)
    val createdAt: Instant? = nowMillis()
) {

    companion object {

        const val COLLECTION = "raw_on_chain_meta_cache"
    }
}
