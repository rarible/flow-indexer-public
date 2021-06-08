package com.rarible.flow.scanner.config

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.kafka.json.JsonSerializer
import com.rarible.flow.events.EventMessage
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableMongoRepositories(basePackages = ["com.rarible.flow.scanner.repo"])
@EnableScheduling
@EnableConfigurationProperties(ScannerProperties::class)
class Config(
    private val scannerProperties: ScannerProperties,
) {
    private val clientId = "${scannerProperties.environment}.flow.nft-scanner"

    @Bean
    fun kafkaProducer(): RaribleKafkaProducer<EventMessage> {
        return RaribleKafkaProducer(
            clientId = clientId,
            valueSerializerClass = JsonSerializer::class.java,
            defaultTopic = EventMessage.getTopic(scannerProperties.environment),
            bootstrapServers = scannerProperties.kafkaReplicaSet
        )
    }
}