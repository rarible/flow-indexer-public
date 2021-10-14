package com.rarible.flow.scanner.config

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.kafka.json.JsonSerializer
import com.rarible.protocol.dto.*


object GatewayEventsProducers {

    fun itemsUpdates(environment: String, kafkaBootstrapServer: String): RaribleKafkaProducer<FlowNftItemEventDto> {
        return RaribleKafkaProducer(
            clientId = "$environment.flow.nft-events-importer",
            valueSerializerClass = JsonSerializer::class.java,
            defaultTopic = FlowNftItemEventTopicProvider.getTopic(environment),
            bootstrapServers = kafkaBootstrapServer
        )
    }

    fun ownershipsUpdates(environment: String, kafkaBootstrapServer: String): RaribleKafkaProducer<FlowOwnershipEventDto> {
        return RaribleKafkaProducer(
            clientId = "$environment.flow.ownership-events-importer",
            valueSerializerClass = JsonSerializer::class.java,
            defaultTopic = FlowNftOwnershipEventTopicProvider.getTopic(environment),
            bootstrapServers = kafkaBootstrapServer
        )
    }

    fun ordersUpdates(environment: String, kafkaBootstrapServer: String): RaribleKafkaProducer<FlowOrderEventDto> {
        return RaribleKafkaProducer(
            clientId = "$environment.flow.order-events-importer",
            valueSerializerClass = JsonSerializer::class.java,
            defaultTopic = FlowOrderEventTopicProvider.getTopic(environment),
            bootstrapServers = kafkaBootstrapServer
        )
    }
}
