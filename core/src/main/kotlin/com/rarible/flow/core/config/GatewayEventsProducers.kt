package com.rarible.flow.core.config

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.kafka.json.JsonSerializer
import com.rarible.protocol.dto.FlowActivityEventDto
import com.rarible.protocol.dto.FlowActivityEventTopicProvider
import com.rarible.protocol.dto.FlowCollectionEventDto
import com.rarible.protocol.dto.FlowNftCollectionEventTopicProvider
import com.rarible.protocol.dto.FlowNftItemEventDto
import com.rarible.protocol.dto.FlowNftItemEventTopicProvider
import com.rarible.protocol.dto.FlowNftOwnershipEventTopicProvider
import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.dto.FlowOrderEventTopicProvider
import com.rarible.protocol.dto.FlowOwnershipEventDto

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

    fun collectionsUpdates(environment: String, kafkaBootstrapServer: String): RaribleKafkaProducer<FlowCollectionEventDto> {
        return RaribleKafkaProducer(
            clientId = "$environment.flow.collection-events-importer",
            valueSerializerClass = JsonSerializer::class.java,
            defaultTopic = FlowNftCollectionEventTopicProvider.getTopic(environment),
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

    fun activitiesUpdates(
        environment: String,
        kafkaBootstrapServer: String
    ): RaribleKafkaProducer<FlowActivityEventDto> {
        return RaribleKafkaProducer(
            clientId = "$environment.flow.activity-events-importer",
            valueSerializerClass = JsonSerializer::class.java,
            defaultTopic = FlowActivityEventTopicProvider.getActivityTopic(environment),
            bootstrapServers = kafkaBootstrapServer
        )
    }
}
