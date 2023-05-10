package com.rarible.flow.core.config

import com.rarible.blockchain.scanner.flow.mongo.FlowFieldConverter
import com.rarible.flow.core.converter.FlowConversions
import com.rarible.flow.core.converter.ItemHistoryToDtoConverter
import com.rarible.flow.core.converter.ItemIdConversions
import com.rarible.flow.core.converter.OwnershipIdConversions
import com.rarible.flow.core.kafka.ProtocolEventPublisher
import com.rarible.flow.core.service.ServicePackage
import org.bson.types.Decimal128
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import java.math.BigDecimal

@Configuration
@EnableReactiveMongoRepositories(
    basePackages = [
        "com.rarible.flow.core.repository",
    ]
)
@EnableConfigurationProperties(
    value = [
        AppProperties::class
    ]
)
@ComponentScan(
    basePackageClasses = [
        ServicePackage::class,
    ]
)
class CoreConfig(
    private val appProperties: AppProperties
) {

    @Bean
    fun mongoCustomConversions(): MongoCustomConversions {
        return MongoCustomConversions(
            FlowConversions + ItemIdConversions + OwnershipIdConversions + decimalConversions() + FlowFieldConverter
        )
    }

    fun decimalConversions() = listOf(
        BigDecimalDecimal128Converter(),
        Decimal128BigDecimalConverter()
    )

    @Bean
    fun itemHistoryToDtoConverter(mongo: MongoTemplate): ItemHistoryToDtoConverter = ItemHistoryToDtoConverter(mongo)

    @Bean
    @Profile("!without-kafka")
    fun protocolEventPublisher(itemHistoryToDtoConverter: ItemHistoryToDtoConverter) = ProtocolEventPublisher(
        GatewayEventsProducers.itemsUpdates(appProperties.environment, appProperties.kafkaReplicaSet),
        GatewayEventsProducers.ownershipsUpdates(appProperties.environment, appProperties.kafkaReplicaSet),
        GatewayEventsProducers.ordersUpdates(appProperties.environment, appProperties.kafkaReplicaSet),
        GatewayEventsProducers.activitiesUpdates(appProperties.environment, appProperties.kafkaReplicaSet),
        GatewayEventsProducers.auctionsUpdates(appProperties.environment, appProperties.kafkaReplicaSet),
        itemHistoryToDtoConverter
    )

    @Bean
    fun featureFlagsProperties(): FeatureFlagsProperties {
        return appProperties.featureFlags
    }
}

@WritingConverter
private class BigDecimalDecimal128Converter : Converter<BigDecimal, Decimal128> {

    override fun convert(source: BigDecimal): Decimal128 {
        return Decimal128(source)
    }
}

@ReadingConverter
private class Decimal128BigDecimalConverter : Converter<Decimal128, BigDecimal> {

    override fun convert(source: Decimal128): BigDecimal {
        return source.bigDecimalValue()
    }
}
