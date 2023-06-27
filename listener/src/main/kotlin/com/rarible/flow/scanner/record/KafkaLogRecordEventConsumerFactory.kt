package com.rarible.flow.scanner.record

import com.rarible.blockchain.scanner.consumer.LogRecordConsumerWorkerFactory
import com.rarible.core.daemon.sequential.ConsumerWorkerHolder
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import com.rarible.flow.scanner.listener.FlowLogListener

object KafkaLogRecordEventConsumerFactory {
    fun createLogRecordEventConsumers(
        factory: LogRecordConsumerWorkerFactory,
        listeners: List<FlowLogListener<*>>,
        workers: Map<String, Int>,
    ): List<RaribleKafkaConsumerWorker<*>> {
        return listeners.map { create(it, factory, workers) }
    }

    private fun <T> create(
        listener: FlowLogListener<T>,
        factory: LogRecordConsumerWorkerFactory,
        workers: Map<String, Int>,
    ): RaribleKafkaConsumerWorker<T> {
        return factory.create(
            listener = listener,
            logRecordType = listener.eventType,
            logRecordMapper = listener.eventMapper,
            logRecordFilters = emptyList(),
            workerCount = workers.getOrDefault(listener.name, 9)
        )
    }
}