package com.rarible.flow.scanner.config

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.kafka.json.JsonSerializer
import com.rarible.flow.events.EventMessage
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableMongoRepositories(basePackages = ["com.rarible.flow.scanner.repo"])
@EnableScheduling
@EnableConfigurationProperties(ScannerProperties::class)
@EnableWebSocketMessageBroker
class Config(
    private val scannerProperties: ScannerProperties,
): WebSocketMessageBrokerConfigurer {
    private val clientId = "${scannerProperties.environment}.flow.nft-scanner"



    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        super.registerStompEndpoints(registry)
        registry.addEndpoint("/topic/block")
        registry.addEndpoint("/topic/tx")
    }

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
