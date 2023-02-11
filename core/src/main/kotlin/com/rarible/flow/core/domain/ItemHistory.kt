package com.rarible.flow.core.domain

import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.flow.model.FlowLogRecord
import java.time.Instant
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.index.IndexDirection
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId

/**
 * NFT Item history (item and order activities)
 * @property id         ID
 * @property date       date of activity
 * @property activity   activity data (see [FlowNftActivity])
 */
@Document("item_history")
@CompoundIndexes(
    CompoundIndex(
        name = "activity_siblings",
        def = "{'activity.type': 1, 'activity.contract': 1, 'activity.tokenId': 1, 'log.transactionHash': 1, 'log.eventIndex': 1, }"
    ),
    CompoundIndex(
        name = "log_uniq",
        def = "{'log.transactionHash': 1, 'log.eventIndex': 1,}",
        unique = true
    )
)
data class ItemHistory(
    @Indexed(direction = IndexDirection.DESCENDING)
    @Field(targetType = FieldType.DATE_TIME)
    val date: Instant,
    val activity: BaseActivity,
    override val log: FlowLog,
    @MongoId(FieldType.STRING)
    val id: String = "${log.transactionHash}.${log.eventIndex}",
): FlowLogRecord() {

    @LastModifiedDate
    @Field(targetType = FieldType.DATE_TIME)
    var updatedAt: Instant = Instant.now()

    override fun getKey() = activity.getKey()

    fun withLog(log: FlowLog): FlowLogRecord = copy(log = log)
}

