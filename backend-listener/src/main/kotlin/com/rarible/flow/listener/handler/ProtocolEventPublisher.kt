package com.rarible.flow.listener.handler

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.KafkaSendResult
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.flow.core.converter.ItemToDtoConverter
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.protocol.dto.*
import java.util.*


class ProtocolEventPublisher(
    private val gatewayKafkaProducer: RaribleKafkaProducer<FlowNftItemEventDto>
) {

    suspend fun onItemUpdate(item: Item): KafkaSendResult {
        return gatewayKafkaProducer.send(
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

    suspend fun onItemDelete(itemId: ItemId): KafkaSendResult {
        return gatewayKafkaProducer.send(
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