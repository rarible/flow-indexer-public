package com.rarible.flow.scanner.model

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.rarible.flow.scanner.FlowEventDeserializer
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * Created by TimochkinEA at 22.05.2021
 */
@Document
data class FlowBlock(
    @Id
    val id: String = "",
    val parentId: String = "",
    val height: Long = 0L,
    val timestamp: Instant = Instant.now(),
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
    val events: List<FlowEvent>
)

data class FlowEvent(
    val type: String,
    val data: String
)

@Document
data class RariEvent(
    @Id
    val id: String,
    val data: String
)

@JsonDeserialize(using = FlowEventDeserializer::class)
data class EventMessage(
    val id: String,
    val fields: Map<String, Any?>
)
