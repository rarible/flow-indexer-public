package com.rarible.flow.scanner.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.kafka.json.JsonSerializer
import com.rarible.flow.events.EventMessage
import com.rarible.flow.json.commonMapper
import com.rarible.flow.scanner.SporkInfo
import com.rarible.flow.scanner.SporkMonitor
import io.grpc.ManagedChannelBuilder
import org.onflow.protobuf.access.AccessAPIGrpc
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ApplicationEventMulticaster
import org.springframework.context.event.SimpleApplicationEventMulticaster
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@Configuration
@EnableMongoRepositories(basePackages = ["com.rarible.flow.scanner.repo"])
@EnableConfigurationProperties(ScannerProperties::class)
class Config(
    private val scannerProperties: ScannerProperties,
) {
    private val clientId = "${scannerProperties.environment}.flow.nft-scanner"

    @Value("\${grpc.client.flow.address}")
    private lateinit var flowNetAddress: String

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
    fun flowMapper(): ObjectMapper = commonMapper()

    @Bean("flowClient")
    fun flowClient(): AccessAPIGrpc.AccessAPIBlockingStub {
        val channel = ManagedChannelBuilder.forTarget(flowNetAddress).usePlaintext().build()
        return AccessAPIGrpc.newBlockingStub(channel)
    }

    @Bean
    fun sporkMonitors(): List<SporkMonitor> =
         scannerProperties.sporks.map { spork ->
            sporkMonitor(spork)
        }

    @Bean
    fun sporkMonitor(sporkInfo: SporkInfo) = SporkMonitor(sporkInfo)

    @Bean(name = ["applicationEventMulticaster"])
    fun simpleApplicationEventMulticaster(): ApplicationEventMulticaster =
        SimpleApplicationEventMulticaster().apply {
            setTaskExecutor(SimpleAsyncTaskExecutor())
        }
}
