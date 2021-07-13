package com.rarible.flow.listener.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.core.kafka.json.JsonSerializer
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.events.EventMessage
import com.rarible.flow.events.NftEvent
import com.rarible.flow.json.commonMapper
import com.rarible.flow.listener.handler.EventHandler
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import com.rarible.protocol.dto.FlowNftItemEventDto
import com.rarible.protocol.dto.FlowNftItemEventTopicProvider
import com.rarible.protocol.dto.FlowNftItemUpdateEventDto
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(ListenerProperties::class)
class Config(
    private val listenerProperties: ListenerProperties
) {

    @Bean
    fun eventConsumer(): RaribleKafkaConsumer<EventMessage> {
        return RaribleKafkaConsumer(
            clientId = "${listenerProperties.environment}.flow.nft-scanner.nft-indexer-item-events-consumer",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = EventMessage::class.java,
            consumerGroup = "flow-listener",
            defaultTopic = EventMessage.getTopic(listenerProperties.environment),
            bootstrapServers = listenerProperties.kafkaReplicaSet
        )
    }

    @Bean
    fun eventMessageHandler(
        itemRepository: ItemRepository,
        ownershipRepository: OwnershipRepository,
        orderRepository: OrderRepository,
        protocolEventPublisher: ProtocolEventPublisher
    ): ConsumerEventHandler<EventMessage> {
        return EventHandler(itemRepository, ownershipRepository, orderRepository, protocolEventPublisher)
    }

    @Bean
    fun eventConsumerWorker(
        eventConsumer: RaribleKafkaConsumer<EventMessage>,
        eventMessageHandler: ConsumerEventHandler<EventMessage>
    ): ConsumerWorker<EventMessage> {
        return ConsumerWorker(
            eventConsumer,
            eventMessageHandler,
            "flow-event-message-consumer-worker"
        )
    }

    @Bean
    fun objectMapper(): ObjectMapper = commonMapper()

    @Bean
    fun gatewayEventsProducer(): RaribleKafkaProducer<FlowNftItemEventDto> {
        return RaribleKafkaProducer(
            clientId = "${listenerProperties.environment}.flow.protocol-erc20-events-importer",
            valueSerializerClass = JsonSerializer::class.java,
            defaultTopic = FlowNftItemEventTopicProvider.getTopic(listenerProperties.environment, "flow"),
            bootstrapServers = listenerProperties.kafkaReplicaSet
        )
    }

    @Bean
    fun protocolEventPublisher(
        gatewayEventsProducer: RaribleKafkaProducer<FlowNftItemEventDto>
    ) = ProtocolEventPublisher(gatewayEventsProducer)

}

