package com.rarible.flow.core.domain

import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.flow.model.FlowLogRecord
import com.rarible.flow.core.event.EventMessage
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId

@Document("flow_log_event")
data class FlowLogEvent(
    override val log: FlowLog,
    @MongoId(FieldType.STRING)
    val id: String = "${log.transactionHash}.${log.eventIndex}",
    val event: EventMessage,
    @Indexed
    val type: FlowLogType,
): FlowLogRecord() {
    fun withLog(log: FlowLog): FlowLogRecord = copy(log = log)
    override fun getKey(): String = event.eventId.contractAddress.toString()
}