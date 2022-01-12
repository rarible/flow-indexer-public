package com.rarible.flow.core.kafka

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.KafkaSendResult
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.flow.core.converter.ItemHistoryToDtoConverter
import com.rarible.flow.core.converter.ItemToDtoConverter
import com.rarible.flow.core.converter.OrderToDtoConverter
import com.rarible.flow.core.converter.OwnershipToDtoConverter
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.NFTActivity
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.log.Log
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.dto.FlowNftDeletedItemDto
import com.rarible.protocol.dto.FlowNftItemDeleteEventDto
import com.rarible.protocol.dto.FlowNftItemEventDto
import com.rarible.protocol.dto.FlowNftItemUpdateEventDto
import com.rarible.protocol.dto.FlowNftOwnershipDeleteEventDto
import com.rarible.protocol.dto.FlowNftOwnershipUpdateEventDto
import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.dto.FlowOrderUpdateEventDto
import com.rarible.protocol.dto.FlowOwnershipEventDto
import java.util.*


class ProtocolEventPublisher(
    private val items: RaribleKafkaProducer<FlowNftItemEventDto>,
    private val ownerships: RaribleKafkaProducer<FlowOwnershipEventDto>,
    private val orders: RaribleKafkaProducer<FlowOrderEventDto>,
    private val activities: RaribleKafkaProducer<FlowActivityDto>
) {

    suspend fun onItemUpdate(item: Item): KafkaSendResult {
        val key = item.id.toString()
        val message = FlowNftItemUpdateEventDto(
            eventId = "${item.id}.${UUID.randomUUID()}",
            itemId = key,
            ItemToDtoConverter.convert(item)
        )
        return send(items, key, message)
    }

    suspend fun onUpdate(ownership: Ownership): KafkaSendResult {
        val key = ownership.id.toString()
        val message = FlowNftOwnershipUpdateEventDto(
            eventId = "${ownership.id}.${UUID.randomUUID()}",
            ownershipId = key,
            OwnershipToDtoConverter.convert(ownership)
        )
        return send(ownerships, key, message)
    }

    suspend fun onDelete(ownership: List<Ownership>) {
        ownership.forEach {
            val key = it.id.toString()
            val message = FlowNftOwnershipDeleteEventDto(
                eventId = "${it.id}.${UUID.randomUUID()}",
                ownershipId = key,
                OwnershipToDtoConverter.convert(it)
            )

            send(ownerships, key, message)
        }
    }

    suspend fun onDelete(ownership: Ownership): KafkaSendResult {
        val key = ownership.id.toString()
        val message = FlowNftOwnershipDeleteEventDto(
            eventId = "${ownership.id}.${UUID.randomUUID()}",
            ownershipId = key,
            OwnershipToDtoConverter.convert(ownership)
        )
        return send(ownerships, key, message)
    }

    suspend fun onOrderUpdate(order: Order, converter: OrderToDtoConverter): KafkaSendResult {
        val orderId = order.id
        val key = orderId.toString()
        val message = FlowOrderUpdateEventDto(
            eventId = "$orderId.${UUID.randomUUID()}",
            orderId = key,
            converter.convert(order)
        )
        return send(orders, key, message)
    }

    suspend fun onOrderUpdate(orders: List<Order>, converter: OrderToDtoConverter): List<KafkaSendResult> {
        return orders.map { this.onOrderUpdate(it, converter) }
    }

    suspend fun onItemDelete(itemId: ItemId): KafkaSendResult {
        val key = itemId.toString()
        val message = FlowNftItemDeleteEventDto(
            eventId = "${itemId}.${UUID.randomUUID()}",
            itemId = key,
            FlowNftDeletedItemDto(key, itemId.contract, itemId.tokenId)
        )
        return send(items, key, message)
    }

    suspend fun activity(history: ItemHistory): KafkaSendResult? {
        return ItemHistoryToDtoConverter.convert(
            history
        )?.let { dto ->
            val a = history.activity as NFTActivity
            return send(
                activities,
                "${a.contract}:${a.tokenId}-${history.activity.timestamp}",
                dto
            )
        }
    }

    private suspend fun <V> send(producer: RaribleKafkaProducer<V>, key: String, message: V): KafkaSendResult {
        logger.info("Sending to kafka: {} [hashCode={}]...", message, message.hashCode())
        val sendResult = producer.send(
            KafkaMessage(key, message)
        )
        when (sendResult) {
            is KafkaSendResult.Success -> logger.debug("Message [hashCode={}] is successfully sent.", message.hashCode())
            is KafkaSendResult.Fail -> logger.error("Failed to send message [hashCode={}]", message.hashCode())
        }
        return sendResult
    }

    companion object {
        val logger by Log()
    }
}
