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
import org.onflow.sdk.FlowAccessApi
import org.onflow.sdk.impl.FlowAccessApiImpl
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@Configuration
@EnableMongoRepositories(basePackages = ["com.rarible.flow.scanner.repo"])
@EnableConfigurationProperties(ScannerProperties::class)
class Config(
    private val scannerProperties: ScannerProperties,
) {

    @Value("\${grpc.client.flow.address}")
    private lateinit var flowNetAddress: String

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

    @Bean("flowApi")
    fun flowApi(): FlowAccessApi {
        val channel = ManagedChannelBuilder.forTarget(flowNetAddress).usePlaintext().build()
        return FlowAccessApiImpl(AccessAPIGrpc.newBlockingStub(channel))
    }

    @Bean
    fun sporkMonitors(flowApi: FlowAccessApi): List<SporkMonitor> =
         scannerProperties.sporks.map { spork ->
            sporkMonitor(spork, flowApi)
        }

    @Bean
    fun sporkMonitor(sporkInfo: SporkInfo, flowApi: FlowAccessApi) = SporkMonitor(sporkInfo, flowApi)
}
