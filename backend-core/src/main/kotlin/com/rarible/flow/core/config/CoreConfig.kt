package com.rarible.flow.core.config

import com.rarible.flow.core.converter.FlowConversions
import com.rarible.flow.core.converter.ItemIdConversions
import com.rarible.flow.core.converter.OwnershipIdConversions
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@Configuration
@EnableReactiveMongoRepositories(basePackages = [
    "com.rarible.flow.core.repository"
])
@EnableConfigurationProperties(AppProperties::class)
class CoreConfig(
    private val appProperties: AppProperties
) {

    @Bean
    fun mongoCustomConversions(): MongoCustomConversions {
        return MongoCustomConversions(
            FlowConversions + ItemIdConversions + OwnershipIdConversions
        )
    }

    @Bean
    fun protocolEventPublisher() = ProtocolEventPublisher(
        GatewayEventsProducers.itemsUpdates(appProperties.environment, appProperties.kafkaReplicaSet),
        GatewayEventsProducers.ownershipsUpdates(appProperties.environment, appProperties.kafkaReplicaSet),
        GatewayEventsProducers.ordersUpdates(appProperties.environment, appProperties.kafkaReplicaSet)
    )
}
