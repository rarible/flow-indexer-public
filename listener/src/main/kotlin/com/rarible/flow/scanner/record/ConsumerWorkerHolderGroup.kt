package com.rarible.flow.scanner.record

import com.rarible.core.daemon.sequential.ConsumerWorkerHolder

class ConsumerWorkerHolderGroup(
    private val holder: List<ConsumerWorkerHolder<*>>
) : KafkaConsumerWorker<Any> {

    override fun start() {
        holder.forEach { it.start() }
    }

    override fun close() {
        holder.forEach { it.close() }
    }
}
