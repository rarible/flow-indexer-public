package com.rarible.flow.listener.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.core.kafka.json.JsonSerializer
import com.rarible.flow.core.config.CoreConfig
import com.rarible.flow.events.EventMessage
import com.rarible.flow.json.commonMapper
import com.rarible.flow.listener.handler.EventHandler
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import com.rarible.flow.listener.handler.listeners.SmartContractEventHandler
import com.rarible.protocol.dto.*
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(CoreConfig::class)
@EnableConfigurationProperties(ListenerProperties::class)
class Config(
    private val listenerProperties: ListenerProperties
) {

    @Bean
    fun eventConsumer(): RaribleKafkaConsumer<EventMessage> {
        return RaribleKafkaConsumer(
            clientId = "${listenerProperties.environment}.flow.nft-listener",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = EventMessage::class.java,
            consumerGroup = "flow-listener",
            defaultTopic = EventMessage.getTopic(listenerProperties.environment),
            bootstrapServers = listenerProperties.kafkaReplicaSet
        )
    }

    @Bean
    fun eventMessageHandler(
        smartContractEventHandlers: Map<String, SmartContractEventHandler>
    ): ConsumerEventHandler<EventMessage> {
        return EventHandler(
            smartContractEventHandlers
        )
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
    fun gatewayItemEventsProducer(): RaribleKafkaProducer<FlowNftItemEventDto> {
        return RaribleKafkaProducer(
            clientId = "${listenerProperties.environment}.flow.nft-events-importer",
            valueSerializerClass = JsonSerializer::class.java,
            defaultTopic = FlowNftItemEventTopicProvider.getTopic(listenerProperties.environment),
            bootstrapServers = listenerProperties.kafkaReplicaSet
        )
    }

    @Bean
    fun gatewayOwnershipEventsProducer(): RaribleKafkaProducer<FlowOwnershipEventDto> {
        return RaribleKafkaProducer(
            clientId = "${listenerProperties.environment}.flow.ownership-events-importer",
            valueSerializerClass = JsonSerializer::class.java,
            defaultTopic = FlowNftOwnershipEventTopicProvider.getTopic(listenerProperties.environment),
            bootstrapServers = listenerProperties.kafkaReplicaSet
        )
    }

    @Bean
    fun gatewayOrderEventsProducer(): RaribleKafkaProducer<FlowOrderEventDto> {
        return RaribleKafkaProducer(
            clientId = "${listenerProperties.environment}.flow.order-events-importer",
            valueSerializerClass = JsonSerializer::class.java,
            defaultTopic = FlowOrderEventTopicProvider.getTopic(listenerProperties.environment),
            bootstrapServers = listenerProperties.kafkaReplicaSet
        )
    }

    @Bean
    fun protocolEventPublisher(
        gatewayItemEventsProducer: RaribleKafkaProducer<FlowNftItemEventDto>,
        gatewayOwnershipEventsProducer: RaribleKafkaProducer<FlowOwnershipEventDto>,
        gatewayOrderEventsProducer: RaribleKafkaProducer<FlowOrderEventDto>,
    ) = ProtocolEventPublisher(
        gatewayItemEventsProducer,
        gatewayOwnershipEventsProducer,
        gatewayOrderEventsProducer
    )

}

