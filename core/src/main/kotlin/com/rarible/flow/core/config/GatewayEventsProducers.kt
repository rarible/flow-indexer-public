package com.rarible.flow.core.config

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

    fun activitiesUpdates(environment: String, kafkaBootstrapServer: String): RaribleKafkaProducer<FlowActivityDto> {
        return RaribleKafkaProducer(
            clientId = "$environment.flow.activity-events-importer",
            valueSerializerClass = JsonSerializer::class.java,
            defaultTopic = FlowActivityEventTopicProvider.getTopic(environment),
            bootstrapServers = kafkaBootstrapServer
        )
    }

    fun auctionsUpdates(environment: String, kafkaBootstrapServer: String): RaribleKafkaProducer<FlowAuctionDto> {
        return RaribleKafkaProducer(
            clientId = "$environment.flow.auction-events-importer",
            valueSerializerClass = JsonSerializer::class.java,
            defaultTopic = FlowActivityEventTopicProvider.getTopic(environment),
            bootstrapServers = kafkaBootstrapServer
        )
    }
}
