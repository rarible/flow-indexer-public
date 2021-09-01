package com.rarible.flow.scanner.model

import com.rarible.flow.events.EventMessage
import org.springframework.context.ApplicationEvent
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.MongoId

@Document
data class RariEventMessage(
    @MongoId
    val messageId: String,
    val event: EventMessage
)

/**
 * In-app event for process rari events
 */
data class RariEventMessageCaught(val message: RariEventMessage): ApplicationEvent(message)
