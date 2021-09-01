package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.events.EventMessage

/**
 * Handles smart-contracts events
 *
 * Inheritors should be registered as @Components with bean name corresponding to cadence event name
 * e.g. `@Component("TopShot.Minted")`.
 * All handlers are supposed to be autowired as Map of (eventName) -> (handlerInstance)
 */
interface SmartContractEventHandler {
    suspend fun handle(eventMessage: EventMessage)
}
