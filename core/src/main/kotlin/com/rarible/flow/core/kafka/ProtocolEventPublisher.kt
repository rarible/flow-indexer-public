package com.rarible.flow.core.kafka

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.flow.core.converter.AuctionToDtoConverter
import com.rarible.flow.core.converter.FlowNftCollectionDtoConverter
import com.rarible.flow.core.converter.ItemHistoryToDtoConverter
import com.rarible.flow.core.converter.ItemToDtoConverter
import com.rarible.flow.core.converter.OrderToDtoConverter
import com.rarible.flow.core.converter.OwnershipToDtoConverter
import com.rarible.flow.core.domain.EnglishAuctionLot
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemCollection
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.util.Log
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.dto.FlowAuctionDto
import com.rarible.protocol.dto.FlowCollectionEventDto
import com.rarible.protocol.dto.FlowCollectionUpdateEventDto
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
    private val collections: RaribleKafkaProducer<FlowCollectionEventDto>,
    private val orders: RaribleKafkaProducer<FlowOrderEventDto>,
    private val activities: RaribleKafkaProducer<FlowActivityDto>,
    private val auctions: RaribleKafkaProducer<FlowAuctionDto>,
    private val itemHistoryToDtoConverter: ItemHistoryToDtoConverter,
) {

    private val logger by Log()

    suspend fun onItemUpdate(item: Item, marks: FlowEventTimeMarksDto) {
        val key = item.id.toString()
        val message = FlowNftItemUpdateEventDto(
            eventId = "${item.id}.${UUID.randomUUID()}",
            itemId = key,
            item = ItemToDtoConverter.convert(item),
            eventTimeMarks = marks.onIndexerOut()
        )
        send(items, key, message)
    }

    suspend fun onUpdate(ownership: Ownership, marks: FlowEventTimeMarksDto) {
        val key = ownership.id.toString()
        val message = FlowNftOwnershipUpdateEventDto(
            eventId = "${ownership.id}.${UUID.randomUUID()}",
            ownershipId = key,
            ownership = OwnershipToDtoConverter.convert(ownership),
            eventTimeMarks = marks.onIndexerOut()
        )
        send(ownerships, key, message)
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

    suspend fun onDelete(ownership: Ownership, marks: FlowEventTimeMarksDto) {
        val key = ownership.id.toString()
        val message = FlowNftOwnershipDeleteEventDto(
            eventId = "${ownership.id}.${UUID.randomUUID()}",
            ownershipId = key,
            ownership = OwnershipToDtoConverter.convert(ownership),
            eventTimeMarks = marks.onIndexerOut()
        )
        send(ownerships, key, message)
    }

    suspend fun onOrderUpdate(
        order: Order,
        converter: OrderToDtoConverter,
        marks: FlowEventTimeMarksDto
    ) {
        val orderId = order.id
        val key = orderId.toString()
        val message = FlowOrderUpdateEventDto(
            eventId = "$orderId.${UUID.randomUUID()}",
            orderId = key,
            order = converter.convert(order),
            eventTimeMarks = marks.onIndexerOut()
        )
        send(orders, key, message)
    }

    suspend fun onItemDelete(itemId: ItemId, marks: FlowEventTimeMarksDto) {
        val key = itemId.toString()
        val message = FlowNftItemDeleteEventDto(
            eventId = "${itemId}.${UUID.randomUUID()}",
            itemId = key,
            item = FlowNftDeletedItemDto(key, itemId.contract, itemId.tokenId),
            eventTimeMarks = marks.onIndexerOut()
        )
        send(items, key, message)
    }

    suspend fun activity(history: ItemHistory, reverted: Boolean = false) {
        send(
            activities,
            "${history.id}:${history.activity.type}-${history.activity.timestamp}",
            itemHistoryToDtoConverter.convert(history, reverted)
        )
    }

    suspend fun auction(auction: EnglishAuctionLot) {
        send(
            auctions,
            "${auction.id}.${UUID.randomUUID()}",
            AuctionToDtoConverter.convert(auction)
        )
    }

    suspend fun onCollection(collection: ItemCollection, marks: FlowEventTimeMarksDto) {
        val key = collection.id
        val dto = FlowNftCollectionDtoConverter.convert(collection)
        val eventId = "$key.${UUID.randomUUID()}"
        val eventTimeMarks = marks.onIndexerOut()

        val message = FlowCollectionUpdateEventDto(
                eventId = eventId,
                collectionId = key,
                collection = dto,
                eventTimeMarks = eventTimeMarks
            )
        send(collections, key, message)
    }

    private suspend fun <V> send(producer: RaribleKafkaProducer<V>, key: String, message: V) {
        logger.info("Sending to kafka: {}...", message)
        producer.send(KafkaMessage(key, message)).ensureSuccess()
    }

    private fun FlowEventTimeMarksDto.onIndexerOut(): FlowEventTimeMarksDto {
        return this.add("indexer-out")
    }
}
