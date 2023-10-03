package com.rarible.flow.core.domain

import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.flow.model.FlowLogRecord
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId
import java.math.BigDecimal
import java.time.Instant

data class BalanceHistory(
    val balanceId: BalanceId,
    val change: BigDecimal,
    @Field(targetType = FieldType.DATE_TIME)
    val date: Instant,
    override val log: FlowLog,
    @MongoId(FieldType.STRING)
    val id: String = "${log.transactionHash}.${log.eventIndex}"
) : FlowLogRecord() {
    fun withLog(log: FlowLog): FlowLogRecord = copy(log = log)
    override fun getKey() = balanceId.toString()
}
