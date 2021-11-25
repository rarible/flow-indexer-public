package com.rarible.flow.core.kafka

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.KafkaSendResult
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.flow.core.converter.ItemHistoryToDtoConverter
import com.rarible.flow.core.converter.ItemToDtoConverter
import com.rarible.flow.core.converter.OrderToDtoConverter
import com.rarible.flow.core.converter.OwnershipToDtoConverter
import com.rarible.flow.core.domain.*
import com.rarible.flow.log.Log
import com.rarible.protocol.dto.*
import java.util.*


class ProtocolEventPublisher(
    private val items: RaribleKafkaProducer<FlowNftItemEventDto>,
    private val ownerships: RaribleKafkaProducer<FlowOwnershipEventDto>,
    private val orders: RaribleKafkaProducer<FlowOrderEventDto>,
    private val activities: RaribleKafkaProducer<FlowActivityDto>
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

    fun onDelete(ownership: List<Ownership>): Flow<KafkaSendResult> {
        val msg: List<KafkaMessage<FlowOwnershipEventDto>> = ownership.map {
            KafkaMessage(
                it.id.toString(),
                FlowNftOwnershipDeleteEventDto(
                    eventId = "${it.id}.${UUID.randomUUID()}",
                    ownershipId = it.id.toString(),
                    OwnershipToDtoConverter.convert(it)
                )
            )
        }
        return ownerships.send(msg)
    }

    suspend fun onDelete(ownership: Ownership): KafkaSendResult {
        return ownerships.send(
            KafkaMessage(
                ownership.id.toString(),
                FlowNftOwnershipDeleteEventDto(
                    eventId = "${ownership.id}.${UUID.randomUUID()}",
                    ownershipId = ownership.id.toString(),
                    OwnershipToDtoConverter.convert(ownership)
                )
            )
        )
    }

    suspend fun onOrderUpdate(order: Order, converter: OrderToDtoConverter): KafkaSendResult {
        val orderId = order.id
        return orders.send(
            KafkaMessage(
                orderId.toString(),
                FlowOrderUpdateEventDto(
                    eventId = "$orderId.${UUID.randomUUID()}",
                    orderId = orderId.toString(),
                    converter.convert(order)
                )
            )
        )
    }

    suspend fun onOrderUpdate(orders: List<Order>, converter: OrderToDtoConverter): List<KafkaSendResult> {
        return orders.map { this.onOrderUpdate(it, converter) }
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
                        itemId.contract,
                        itemId.tokenId
                    )
                )
            )
        )
    }



    suspend fun activity(history: ItemHistory): KafkaSendResult? {
        return ItemHistoryToDtoConverter.convert(
            history
        )?.let { dto ->
            return activities.send(
                KafkaMessage(
                    "${history.activity.contract}:${history.activity.tokenId}-${history.activity.timestamp}",
                    dto
                )
            )
        }
    }

    companion object {
        val log by Log()
    }
}
