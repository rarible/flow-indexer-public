package com.rarible.flow.scanner.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.blockchain.scanner.flow.EnableFlowBlockchainScanner
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.kafka.json.JsonSerializer
import com.rarible.flow.events.EventMessage
import com.rarible.flow.json.commonMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@Configuration
@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
@FlowPreview
@EnableFlowBlockchainScanner
@EnableReactiveMongoRepositories(basePackages = ["com.rarible.flow.scanner.repo"])
@EnableConfigurationProperties(ScannerProperties::class)
class Config(
    private val scannerProperties: ScannerProperties,
) {

    @Bean
    fun kafkaProducer(): RaribleKafkaProducer<EventMessage> {
        return RaribleKafkaProducer(
            clientId = "${scannerProperties.environment}.flow.nft-scanner",
            valueSerializerClass = JsonSerializer::class.java,
            defaultTopic = EventMessage.getTopic(scannerProperties.environment),
            bootstrapServers = scannerProperties.kafkaReplicaSet
        )
    }

    @Bean
    fun flowMapper(): ObjectMapper = commonMapper()
}
