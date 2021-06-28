package com.rarible.flow.scanner.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.kafka.json.JsonSerializer
import com.rarible.flow.events.EventMessage
import com.rarible.flow.scanner.FlowEventDeserializer
import io.grpc.ManagedChannelBuilder
import org.onflow.protobuf.access.AccessAPIGrpc
import org.springframework.beans.factory.annotation.Value
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

    @Value("\${grpc.client.flow.address}")
    private lateinit var flowNetAddress: String

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

    @Bean
    fun flowMapper(): ObjectMapper {
        val mapper = ObjectMapper()
        mapper.registerKotlinModule()

        val module = SimpleModule()
        module.addDeserializer(EventMessage::class.java, FlowEventDeserializer())
        mapper.registerModule(module)
        return mapper
    }

    @Bean("flowClient")
    fun flowClient(): AccessAPIGrpc.AccessAPIBlockingStub {
        val channel = ManagedChannelBuilder.forTarget(flowNetAddress).usePlaintext().build()
        return AccessAPIGrpc.newBlockingStub(channel)
    }
}
