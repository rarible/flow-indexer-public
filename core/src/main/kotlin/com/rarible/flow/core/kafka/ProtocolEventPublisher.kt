package com.rarible.flow.core.kafka

import com.rarible.core.common.EventTimeMarks
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
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
import com.rarible.flow.core.util.addIndexerOut
import com.rarible.flow.core.util.toDto
import com.rarible.protocol.dto.FlowActivityEventDto
import com.rarible.protocol.dto.FlowCollectionEventDto
import com.rarible.protocol.dto.FlowCollectionUpdateEventDto
import com.rarible.protocol.dto.FlowNftDeletedItemDto
import com.rarible.protocol.dto.FlowNftItemDeleteEventDto
import com.rarible.protocol.dto.FlowNftItemEventDto
import com.rarible.protocol.dto.FlowNftItemUpdateEventDto
import com.rarible.protocol.dto.FlowNftOwnershipDeleteEventDto
import com.rarible.protocol.dto.FlowNftOwnershipUpdateEventDto
import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.dto.FlowOrderUpdateEventDto
import com.rarible.protocol.dto.FlowOwnershipEventDto
import org.slf4j.LoggerFactory
import java.util.UUID

class ProtocolEventPublisher(
    private val items: RaribleKafkaProducer<FlowNftItemEventDto>,
    private val ownerships: RaribleKafkaProducer<FlowOwnershipEventDto>,
    private val collections: RaribleKafkaProducer<FlowCollectionEventDto>,
    private val orders: RaribleKafkaProducer<FlowOrderEventDto>,
    private val activities: RaribleKafkaProducer<FlowActivityEventDto>,
    private val itemHistoryToDtoConverter: ItemHistoryToDtoConverter,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun onItemUpdate(item: Item, marks: EventTimeMarks) {
        val key = item.id.toString()
        val message = FlowNftItemUpdateEventDto(
            eventId = "${item.id}.${UUID.randomUUID()}",
            itemId = key,
            item = ItemToDtoConverter.convert(item),
            eventTimeMarks = marks.addIndexerOut().toDto()
        )
        send(items, key, message)
    }

    suspend fun onUpdate(ownership: Ownership, marks: EventTimeMarks) {
        val key = ownership.id.toString()
        val message = FlowNftOwnershipUpdateEventDto(
            eventId = "${ownership.id}.${UUID.randomUUID()}",
            ownershipId = key,
            ownership = OwnershipToDtoConverter.convert(ownership),
            eventTimeMarks = marks.addIndexerOut().toDto()
        )
        send(ownerships, key, message)
    }

    suspend fun onDelete(ownership: List<Ownership>, marks: EventTimeMarks) {
        ownership.forEach {
            val key = it.id.toString()
            val message = FlowNftOwnershipDeleteEventDto(
                eventId = "${it.id}.${UUID.randomUUID()}",
                ownershipId = key,
                ownership = OwnershipToDtoConverter.convert(it),
                eventTimeMarks = marks.addIndexerOut().toDto()
            )
            send(ownerships, key, message)
        }
    }

    suspend fun onDelete(ownership: Ownership, marks: EventTimeMarks) {
        val key = ownership.id.toString()
        val message = FlowNftOwnershipDeleteEventDto(
            eventId = "${ownership.id}.${UUID.randomUUID()}",
            ownershipId = key,
            ownership = OwnershipToDtoConverter.convert(ownership),
            eventTimeMarks = marks.addIndexerOut().toDto()
        )
        send(ownerships, key, message)
    }

    suspend fun onOrderUpdate(
        order: Order,
        converter: OrderToDtoConverter,
        marks: EventTimeMarks,
    ) {
        val orderId = order.id
        val key = orderId.toString()
        val message = FlowOrderUpdateEventDto(
            eventId = "$orderId.${UUID.randomUUID()}",
            orderId = key,
            order = converter.convert(order),
            eventTimeMarks = marks.addIndexerOut().toDto()
        )
        send(orders, key, message)
    }

    suspend fun onItemDelete(itemId: ItemId, marks: EventTimeMarks) {
        val key = itemId.toString()
        val message = FlowNftItemDeleteEventDto(
            eventId = "$itemId.${UUID.randomUUID()}",
            itemId = key,
            item = FlowNftDeletedItemDto(key, itemId.contract, itemId.tokenId),
            eventTimeMarks = marks.addIndexerOut().toDto()
        )
        send(items, key, message)
    }

    suspend fun activity(
        history: ItemHistory,
        reverted: Boolean = false,
        eventTimeMarks: EventTimeMarks
    ) {
        val message = FlowActivityEventDto(
            activity = itemHistoryToDtoConverter.convert(history, reverted),
            eventTimeMarks = eventTimeMarks.addIndexerOut().toDto()
        )
        send(
            activities,
            "${history.id}:${history.activity.type}-${history.activity.timestamp}",
            message
        )
    }

    suspend fun auction(auction: EnglishAuctionLot) {
        // TODO remove it completely?
        /*send(
            auctions,
            "${auction.id}.${UUID.randomUUID()}",
            AuctionToDtoConverter.convert(auction)
        )*/
    }

    suspend fun onCollection(collection: ItemCollection, marks: EventTimeMarks) {
        val key = collection.id
        val dto = FlowNftCollectionDtoConverter.convert(collection)
        val eventId = "$key.${UUID.randomUUID()}"
        val eventTimeMarks = marks.addIndexerOut().toDto()

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
}
