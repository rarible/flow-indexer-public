package com.rarible.flow.scanner.config

import com.rarible.blockchain.scanner.configuration.KafkaProperties
import com.rarible.blockchain.scanner.consumer.LogRecordConsumerWorkerFactory
import com.rarible.blockchain.scanner.consumer.kafka.KafkaLogRecordConsumerWorkerFactory
import com.rarible.blockchain.scanner.flow.configuration.FlowBlockchainScannerProperties
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import com.rarible.flow.scanner.listener.FlowLogListener
import com.rarible.flow.scanner.record.KafkaLogRecordEventConsumerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ScannerConsumerConfiguration(
    private val flowBlockchainScannerProperties: FlowBlockchainScannerProperties,
    private val flowBlockchainKafkaProperties: KafkaProperties,
    private val flowListenerProperties: FlowListenerProperties,
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
            service = flowBlockchainScannerProperties.service,
        )
    }

    @Bean
    fun logRecordEventConsumerGroup(
        properties: FlowListenerProperties,
        factory: LogRecordConsumerWorkerFactory,
        listeners: List<FlowLogListener<*>>,
    ): RaribleKafkaConsumerWorker<Any> {
        val consumers = KafkaLogRecordEventConsumerFactory.createLogRecordEventConsumers(
            factory = factory,
            listeners = listeners,
            workers = properties.scannerLogRecordListeners
        )
        return object : RaribleKafkaConsumerWorker<Any> {
            override fun close() {
                consumers.forEach { it.close() }
            }

            override fun start() {
                consumers.forEach { it.start() }
            }
        }
    }
}
