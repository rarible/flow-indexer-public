package com.rarible.flow.listener

import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.flow.listener.config.GatewayEventsProducers
import com.rarible.flow.listener.config.ListenerProperties
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import com.rarible.protocol.dto.FlowNftItemEventDto
import com.rarible.protocol.dto.FlowNftItemEventTopicProvider
import com.rarible.protocol.dto.FlowNftOwnershipEventTopicProvider
import com.rarible.protocol.dto.FlowOwnershipEventDto
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class TestPropertiesConfiguration(
    val listenerProperties: ListenerProperties
) {
    @Bean
    fun itemProtocolEvents() = RaribleKafkaConsumer<FlowNftItemEventDto>(
        clientId = "test-items",
        valueDeserializerClass = JsonDeserializer::class.java,
        defaultTopic = FlowNftItemEventTopicProvider.getTopic("test"),
        bootstrapServers = listenerProperties.kafkaReplicaSet,
        consumerGroup = "test-items-group",
        offsetResetStrategy = OffsetResetStrategy.EARLIEST
    )

    @Bean
    fun ownershipProtocolEvents() = RaribleKafkaConsumer<FlowOwnershipEventDto>(
        clientId = "test-ownerships",
        valueDeserializerClass = JsonDeserializer::class.java,
        defaultTopic = FlowNftOwnershipEventTopicProvider.getTopic("test"),
        bootstrapServers = listenerProperties.kafkaReplicaSet,
        consumerGroup = "test-ownerships-group",
        offsetResetStrategy = OffsetResetStrategy.EARLIEST
    )
}
