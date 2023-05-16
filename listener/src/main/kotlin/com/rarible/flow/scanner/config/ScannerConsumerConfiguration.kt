package com.rarible.flow.scanner.config

import com.rarible.blockchain.scanner.configuration.KafkaProperties
import com.rarible.blockchain.scanner.consumer.LogRecordConsumerWorkerFactory
import com.rarible.blockchain.scanner.consumer.kafka.KafkaLogRecordConsumerWorkerFactory
import com.rarible.blockchain.scanner.flow.configuration.FlowBlockchainScannerProperties
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.flow.scanner.listener.FlowLogListener
import com.rarible.flow.scanner.record.ConsumerWorkerHolderGroup
import com.rarible.flow.scanner.record.KafkaLogRecordEventConsumerFactory
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ScannerConsumerConfiguration(
    private val flowBlockchainScannerProperties: FlowBlockchainScannerProperties,
    private val flowBlockchainKafkaProperties: KafkaProperties,
    private val flowListenerProperties: FlowListenerProperties,
    private val meterRegistry: MeterRegistry,
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {
    @Bean
    fun consumerWorkerFactory(): LogRecordConsumerWorkerFactory {
        return KafkaLogRecordConsumerWorkerFactory(
            blockchain = flowBlockchainScannerProperties.blockchain,
            host = applicationEnvironmentInfo.host,
            environment = applicationEnvironmentInfo.name,
            properties = flowBlockchainKafkaProperties,
            daemonProperties = flowListenerProperties.scannerLogRecordDaemon,
            meterRegistry = meterRegistry,
            service = flowBlockchainScannerProperties.service,
        )
    }

    @Bean
    fun logRecordEventConsumerGroup(
        properties: FlowListenerProperties,
        factory: LogRecordConsumerWorkerFactory,
        listeners: List<FlowLogListener<*>>,
    ): ConsumerWorkerHolderGroup {
        return ConsumerWorkerHolderGroup(
            KafkaLogRecordEventConsumerFactory.createLogRecordEventConsumers(
                factory = factory,
                listeners = listeners,
                workers = properties.scannerLogRecordListeners
            )
        )
    }
}
