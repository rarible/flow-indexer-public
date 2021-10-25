package com.rarible.flow.scanner.config

import com.rarible.blockchain.scanner.flow.EnableFlowBlockchainScanner
import com.rarible.flow.scanner.ProtocolEventPublisher
import com.rarible.protocol.currency.api.client.CurrencyApiClientFactory
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ObsoleteCoroutinesApi
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

    @Bean
    fun protocolEventPublisher() = ProtocolEventPublisher(
        GatewayEventsProducers.itemsUpdates(scannerProperties.environment, scannerProperties.kafkaReplicaSet),
        GatewayEventsProducers.ownershipsUpdates(scannerProperties.environment, scannerProperties.kafkaReplicaSet),
        GatewayEventsProducers.ordersUpdates(scannerProperties.environment, scannerProperties.kafkaReplicaSet)
    )

    @Bean
    fun currencyApi(factory: CurrencyApiClientFactory): CurrencyControllerApi {
        return factory.createCurrencyApiClient()
    }
}
