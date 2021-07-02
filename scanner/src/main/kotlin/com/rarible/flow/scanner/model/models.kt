package com.rarible.flow.scanner.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.MongoId
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Created by TimochkinEA at 22.05.2021
 */
@Document
data class FlowBlock(
    @MongoId
    val id: String = "",
    val parentId: String = "",
    @Indexed(unique = true)
    val height: Long = 0L,
    val timestamp: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
    var collectionsCount: Int = 0,
    var transactionsCount: Int = 0
)

@Document
data class FlowTransaction(
    @MongoId
    val id: String,
    val referenceBlockId: String,
    val blockHeight: Long,
    val proposer: String,
    val payer: String,
    val authorizers: List<String>,
    val script: String,
    val events: List<FlowEvent>
)

data class FlowEvent(
    val type: String,
    val data: String,
    val timestamp: LocalDateTime
)

@Document
data class RariEvent(
    @Id
    val id: String,
    val data: String
)
