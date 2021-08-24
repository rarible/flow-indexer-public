package com.rarible.flow.listener.handler

import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.events.EventId
import com.rarible.flow.events.EventMessage
import com.rarible.flow.listener.handler.listeners.SmartContractEventHandler
import com.rarible.flow.log.Log


class EventHandler(
    private val smartContractEventHandlers: Map<String, SmartContractEventHandler<*>>
) : ConsumerEventHandler<EventMessage> {

    override suspend fun handle(event: EventMessage) {
        val contract = event.eventId.contractName
        val tokenId = tokenId(event)

        if(tokenId == null) {
            log.warn("Event [${event.eventId}] has no tokenId")
        } else {
            try {
                log.info("Handling [${event.eventId.contractEvent()}] at [$contract.$tokenId] with fields [${event.fields}]")
                smartContractEventHandlers.getOrDefault(
                    event.eventId.contractEvent(),
                    NoOpHandler(event.eventId)
                ).handle(contract, tokenId, event.fields, event.blockInfo)
            } catch (e: Exception) {
                log.error("Failed to handle message: {}", event, e)
            }
        }
    }

    private fun tokenId(event: EventMessage): TokenId? {
        //todo replace with Flow unmarshalling
        return when(val id = event.fields["id"] ?: event.fields["saleOfferResourceID"]) {
            is String -> id.toLong()
            is Number -> id.toLong()
            else -> null
        }
    }


    companion object {
        val log by Log()
        class NoOpHandler(val eventId: EventId): SmartContractEventHandler<Unit> {
            override suspend fun handle(
                contract: String,
                tokenId: TokenId,
                fields: Map<String, Any?>,
                blockInfo: BlockInfo
            ) {
                log.info("Skipping unknown or untracked event [$eventId] for [$contract.$tokenId] with fields [${fields}]")
            }
        }
    }
}
