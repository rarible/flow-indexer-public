package com.rarible.flow.core.kafka

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.KafkaSendResult
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.flow.core.converter.ItemToDtoConverter
import com.rarible.flow.core.converter.OrderToDtoConverter
import com.rarible.flow.core.converter.OwnershipToDtoConverter
import com.rarible.flow.core.domain.BaseActivity
import com.rarible.flow.core.domain.FlowActivity
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.toDto
import com.rarible.flow.log.Log
import com.rarible.protocol.dto.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    suspend fun onUpdate(order: Order): KafkaSendResult {
        val orderId = order.id
        return orders.send(
            KafkaMessage(
                orderId.toString(),
                FlowOrderUpdateEventDto(
                    eventId = "$orderId.${UUID.randomUUID()}",
                    orderId = orderId.toString(),
                    OrderToDtoConverter.convert(order)
                )
            )
        )
    }

    suspend fun onUpdate(orders: Flow<Order>): Flow<KafkaSendResult> {
        return orders.map { this.onUpdate(it) }
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

    suspend fun activity(activity: BaseActivity, history: ItemHistory): KafkaSendResult {
        log.debug("Sending activity: {}", activity)
        return activities.send(
            KafkaMessage(
                "${activity.contract}:${activity.tokenId}-${activity.timestamp}",
                activity.toDto(history)
            )
        )
    }

    companion object {
        val log by Log()
    }
}
