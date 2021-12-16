package com.rarible.flow.core.domain

import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.flow.model.FlowLogRecord
import com.rarible.flow.events.EventMessage
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
): FlowLogRecord<FlowLogEvent>() {
    override fun withLog(log: FlowLog): FlowLogRecord<FlowLogEvent> = copy(log = log)
}


enum class FlowLogType {

    MINT, WITHDRAW, DEPOSIT, BURN, LISTING_AVAILABLE, LISTING_COMPLETED, BID_AVAILABLE, BID_COMPLETED
}