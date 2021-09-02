package com.rarible.flow.listener.handler

import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.flow.events.EventId
import com.rarible.flow.events.EventMessage
import com.rarible.flow.listener.handler.listeners.SmartContractEventHandler
import com.rarible.flow.log.Log


class EventHandler(
    private val smartContractEventHandlers: Map<String, SmartContractEventHandler>
) : ConsumerEventHandler<EventMessage> {

    override suspend fun handle(event: EventMessage) {
        try {
            log.info("Handling event: $event")
            smartContractEventHandlers.getOrDefault(
                event.eventId.contractEvent(),
                NoOpHandler(event.eventId)
            ).handle(event)
        } catch (e: Exception) {
            log.error("Failed to handle message: {}", event, e)
        }

    }

    companion object {
        val log by Log()
        class NoOpHandler(val eventId: EventId): SmartContractEventHandler {
            override suspend fun handle(
                event: EventMessage
            ) {
                log.info("Skipping unknown or untracked event [$eventId]: [${event}]")
            }
        }
    }
}
