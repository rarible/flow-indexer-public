package com.rarible.flow.scanner.test

import com.rarible.core.kafka.RaribleKafkaEventHandler
import java.util.concurrent.ConcurrentLinkedDeque

class TestFlowEventHandler<T> : RaribleKafkaEventHandler<T> {

    val events = ConcurrentLinkedDeque<T>()

    override suspend fun handle(event: T) {
        events.add(event)
    }
}