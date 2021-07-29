package com.rarible.flow.listener.handler

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.KafkaSendResult
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.flow.core.converter.ItemToDtoConverter
import com.rarible.flow.core.converter.OwnershipToDtoConverter
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Ownership
import com.rarible.protocol.dto.*
import java.util.*


class ProtocolEventPublisher(
    private val items: RaribleKafkaProducer<FlowNftItemEventDto>,
    private val ownerships: RaribleKafkaProducer<FlowOwnershipEventDto>
) {

    suspend fun onItemUpdate(item: Item): KafkaSendResult {
        return items.send(
            KafkaMessage(
                item.id.toString(),
                FlowNftItemUpdateEventDto(
                    eventId = "${item.id}.${UUID.randomUUID()}",
                    itemId = item.id.toString(),
                    ItemToDtoConverter.convert(item)

                )
            )
        )
    }

    suspend fun onUpdate(ownership: Ownership): KafkaSendResult {
        return ownerships.send(
            KafkaMessage(
                ownership.id.toString(),
                FlowNftOwnershipUpdateEventDto(
                    eventId = "${ownership.id}.${UUID.randomUUID()}",
                    ownershipId = ownership.id.toString(),
                    OwnershipToDtoConverter.convert(ownership)
                )
            )
        )
    }

    suspend fun onItemDelete(itemId: ItemId): KafkaSendResult {
        return items.send(
            KafkaMessage(
                itemId.toString(),
                FlowNftItemDeleteEventDto(
                    eventId = "${itemId}.${UUID.randomUUID()}",
                    itemId = itemId.toString(),
                    FlowNftDeletedItemDto(
                        itemId.toString(),
                        itemId.contract.formatted,
                        itemId.tokenId.toInt() //todo long
                    )
                )
            )
        )
    }
}