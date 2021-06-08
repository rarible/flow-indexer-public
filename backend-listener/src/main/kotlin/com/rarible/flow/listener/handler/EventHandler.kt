package com.rarible.flow.listener.handler

import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.flow.core.domain.Address
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.events.EventMessage
import java.time.Instant


class EventHandler(private val itemRepository: ItemRepository) : ConsumerEventHandler<EventMessage> {

    override suspend fun handle(event: EventMessage) {
        val existingEvent = itemRepository.findById(event.id)
        if (existingEvent == null) {
            val address = Address(event.id.split('.')[1])
            itemRepository.save(
                Item(
                    address.value,
                    event.fields["id"]!!.toInt(),
                    address,
                    emptyList(),
                    address,
                    Instant.now(),
                    1000, //TODO fix
                    event.fields
                )
            )
        }
    }

}