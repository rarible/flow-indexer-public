package com.rarible.flow.scanner.config

import com.rarible.blockchain.scanner.flow.EnableFlowBlockchainScanner
import com.rarible.flow.scanner.ProtocolEventPublisher
import io.grpc.ClientInterceptor
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ObsoleteCoroutinesApi
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
@FlowPreview
@EnableFlowBlockchainScanner
@EnableConfigurationProperties(ScannerProperties::class)
class Config(
    private val scannerProperties: ScannerProperties,
) {

    @GrpcGlobalClientInterceptor
    fun alchemyApiKeyHeaderInterceptor(): ClientInterceptor {
        val header = Metadata()
        val key = Metadata.Key.of("api_key", Metadata.ASCII_STRING_MARSHALLER)
        header.put(key, scannerProperties.alchemyApiKey)
        return MetadataUtils.newAttachHeadersInterceptor(header)
    }

    @Bean
    fun protocolEventPublisher() = ProtocolEventPublisher(
        GatewayEventsProducers.itemsUpdates(scannerProperties.environment, scannerProperties.kafkaReplicaSet),
        GatewayEventsProducers.ownershipsUpdates(scannerProperties.environment, scannerProperties.kafkaReplicaSet),
        GatewayEventsProducers.ordersUpdates(scannerProperties.environment, scannerProperties.kafkaReplicaSet)
    )
}
