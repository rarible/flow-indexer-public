package com.rarible.flow.scanner.record

interface KafkaConsumerWorker<T> : AutoCloseable {

    fun start()
}
