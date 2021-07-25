package com.rarible.flow.core.domain

import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.MongoId
import java.time.LocalDateTime

/**
 * NFT Item history (item and order activities)
 * @property id         ID
 * @property date       date of activity
 * @property activity   activity data (see [FlowActivity])
 */
@Document
data class ItemHistory(
    @MongoId
    val id: String,
    val date: LocalDateTime,
    val activity: FlowActivity
)
