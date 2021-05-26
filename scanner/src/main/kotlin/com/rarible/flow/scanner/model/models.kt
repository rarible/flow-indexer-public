package com.rarible.flow.scanner.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * Created by TimochkinEA at 22.05.2021
 */
@Document
data class FlowBlock(
    @Id
    val id: String,
    val parentId: String,
    val height: Long,
    val timestamp: Instant,
    var collectionsCount: Int = 0,
    var transactionsCount: Int = 0
)

@Document
data class FlowTransaction(
    @Id
    val id: String,
    val blockHeight: Long,
    val proposer: String,
    val payer: String,
    val authorizers: List<String>,
    val script: String,
    val events: MutableList<FlowEvent> = mutableListOf()
)

data class FlowEvent(
    val type: String,
    val data: String
)
