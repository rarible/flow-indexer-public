package com.rarible.flow.core.config

import com.rarible.core.application.ApplicationEnvironmentInfo
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
import org.springframework.stereotype.Component

@Component
class GatewayEventsProducers(
    private val appProperties: AppProperties,
    applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {

    private val env = applicationEnvironmentInfo.name

    fun itemsUpdates(): RaribleKafkaProducer<FlowNftItemEventDto> {
        return RaribleKafkaProducer(
            clientId = "$env.flow.nft-events-importer",
            valueSerializerClass = JsonSerializer::class.java,
            defaultTopic = FlowNftItemEventTopicProvider.getTopic(env),
            bootstrapServers = appProperties.kafkaReplicaSet,
            compression = appProperties.compression,
        )
    }

    fun ownershipsUpdates(): RaribleKafkaProducer<FlowOwnershipEventDto> {
        return RaribleKafkaProducer(
            clientId = "$env.flow.ownership-events-importer",
            valueSerializerClass = JsonSerializer::class.java,
            defaultTopic = FlowNftOwnershipEventTopicProvider.getTopic(env),
            bootstrapServers = appProperties.kafkaReplicaSet,
            compression = appProperties.compression,
        )
    }

    fun collectionsUpdates(): RaribleKafkaProducer<FlowCollectionEventDto> {
        return RaribleKafkaProducer(
            clientId = "$env.flow.collection-events-importer",
            valueSerializerClass = JsonSerializer::class.java,
            defaultTopic = FlowNftCollectionEventTopicProvider.getTopic(env),
            bootstrapServers = appProperties.kafkaReplicaSet,
            compression = appProperties.compression,
        )
    }

    fun ordersUpdates(): RaribleKafkaProducer<FlowOrderEventDto> {
        return RaribleKafkaProducer(
            clientId = "$env.flow.order-events-importer",
            valueSerializerClass = JsonSerializer::class.java,
            defaultTopic = FlowOrderEventTopicProvider.getTopic(env),
            bootstrapServers = appProperties.kafkaReplicaSet,
            compression = appProperties.compression,
        )
    }

    fun activitiesUpdates(): RaribleKafkaProducer<FlowActivityEventDto> {
        return RaribleKafkaProducer(
            clientId = "$env.flow.activity-events-importer",
            valueSerializerClass = JsonSerializer::class.java,
            defaultTopic = FlowActivityEventTopicProvider.getActivityTopic(env),
            bootstrapServers = appProperties.kafkaReplicaSet,
            compression = appProperties.compression,
        )
    }
}
