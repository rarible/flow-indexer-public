package com.rarible.flow.core.kafka

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.KafkaSendResult
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.flow.core.converter.AuctionToDtoConverter
import com.rarible.flow.core.converter.ItemHistoryToDtoConverter
import com.rarible.flow.core.converter.ItemToDtoConverter
import com.rarible.flow.core.converter.OrderToDtoConverter
import com.rarible.flow.core.converter.OwnershipToDtoConverter
import com.rarible.flow.core.domain.EnglishAuctionLot
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.OwnershipId
import com.rarible.flow.core.util.Log
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.dto.FlowAuctionDto
import com.rarible.protocol.dto.FlowEventTimeMarksDto
import com.rarible.protocol.dto.FlowNftDeletedItemDto
import com.rarible.protocol.dto.FlowNftItemDeleteEventDto
import com.rarible.protocol.dto.FlowNftItemEventDto
import com.rarible.protocol.dto.FlowNftItemUpdateEventDto
import com.rarible.protocol.dto.FlowNftOwnershipDeleteEventDto
import com.rarible.protocol.dto.FlowNftOwnershipUpdateEventDto
import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.dto.FlowOrderUpdateEventDto
import com.rarible.protocol.dto.FlowOwnershipEventDto
import com.rarible.protocol.dto.add
import java.util.UUID

class ProtocolEventPublisher(
    private val items: RaribleKafkaProducer<FlowNftItemEventDto>,
    private val ownerships: RaribleKafkaProducer<FlowOwnershipEventDto>,
    private val orders: RaribleKafkaProducer<FlowOrderEventDto>,
    private val activities: RaribleKafkaProducer<FlowActivityDto>,
    private val auctions: RaribleKafkaProducer<FlowAuctionDto>,
    private val itemHistoryToDtoConverter: ItemHistoryToDtoConverter
) {

    private val logger by Log()

    suspend fun onItemUpdate(item: Item, marks: FlowEventTimeMarksDto): KafkaSendResult {
        val key = item.id.toString()
        val message = FlowNftItemUpdateEventDto(
            eventId = "${item.id}.${UUID.randomUUID()}",
            itemId = key,
            item = ItemToDtoConverter.convert(item),
            eventTimeMarks = marks.onIndexerOut()
        )
        return send(items, key, message)
    }

    suspend fun onUpdate(ownership: Ownership, marks: FlowEventTimeMarksDto): KafkaSendResult {
        val key = ownership.id.toString()
        val message = FlowNftOwnershipUpdateEventDto(
            eventId = "${ownership.id}.${UUID.randomUUID()}",
            ownershipId = key,
            ownership = OwnershipToDtoConverter.convert(ownership),
            eventTimeMarks = marks.onIndexerOut()
        )
        return send(ownerships, key, message)
    }

    suspend fun onDelete(ownership: List<Ownership>, marks: FlowEventTimeMarksDto) {
        ownership.forEach {
            val key = it.id.toString()
            val message = FlowNftOwnershipDeleteEventDto(
                eventId = "${it.id}.${UUID.randomUUID()}",
                ownershipId = key,
                ownership = OwnershipToDtoConverter.convert(it),
                eventTimeMarks = marks.onIndexerOut()
            )

            send(ownerships, key, message)
        }
    }

    suspend fun onDelete(ownership: Ownership, marks: FlowEventTimeMarksDto): KafkaSendResult {
        val key = ownership.id.toString()
        val message = FlowNftOwnershipDeleteEventDto(
            eventId = "${ownership.id}.${UUID.randomUUID()}",
            ownershipId = key,
            ownership = OwnershipToDtoConverter.convert(ownership),
            eventTimeMarks = marks.onIndexerOut()
        )
        return send(ownerships, key, message)
    }

    suspend fun onOrderUpdate(
        order: Order,
        converter: OrderToDtoConverter,
        marks: FlowEventTimeMarksDto
    ): KafkaSendResult {
        val orderId = order.id
        val key = orderId.toString()
        val message = FlowOrderUpdateEventDto(
            eventId = "$orderId.${UUID.randomUUID()}",
            orderId = key,
            order = converter.convert(order),
            eventTimeMarks = marks.onIndexerOut()
        )
        return send(orders, key, message)
    }

    suspend fun onItemDelete(itemId: ItemId, marks: FlowEventTimeMarksDto): KafkaSendResult {
        val key = itemId.toString()
        val message = FlowNftItemDeleteEventDto(
            eventId = "${itemId}.${UUID.randomUUID()}",
            itemId = key,
            item = FlowNftDeletedItemDto(key, itemId.contract, itemId.tokenId),
            eventTimeMarks = marks.onIndexerOut()
        )
        return send(items, key, message)
    }

    suspend fun activity(history: ItemHistory): KafkaSendResult {
        return send(
            activities,
            "${history.id}:${history.activity.type}-${history.activity.timestamp}",
            itemHistoryToDtoConverter.convert(history)
        )
    }

    suspend fun auction(auction: EnglishAuctionLot): KafkaSendResult {
        return send(
            auctions,
            "${auction.id}.${UUID.randomUUID()}",
            AuctionToDtoConverter.convert(auction)
        )
    }

    private suspend fun <V> send(producer: RaribleKafkaProducer<V>, key: String, message: V): KafkaSendResult {
        logger.info("Sending to kafka: {} [hashCode={}]...", message, message.hashCode())
        val sendResult = producer.send(
            KafkaMessage(key, message)
        )
        when (sendResult) {
            is KafkaSendResult.Success -> logger.debug(
                "Message [hashCode={}] is successfully sent.",
                message.hashCode()
            )

            is KafkaSendResult.Fail -> logger.error("Failed to send message [hashCode={}]", message.hashCode())
        }
        return sendResult
    }

    private fun FlowEventTimeMarksDto.onIndexerOut(): FlowEventTimeMarksDto {
        return this.add("indexer-out")
    }

}
