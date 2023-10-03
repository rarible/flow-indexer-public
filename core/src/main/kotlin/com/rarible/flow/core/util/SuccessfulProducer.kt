package com.rarible.flow.core.util

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.KafkaProducer
import com.rarible.core.kafka.KafkaSendResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

class SuccessfulProducer<T> : KafkaProducer<T> {
    private val result = KafkaSendResult.Success("1")

    override suspend fun send(message: KafkaMessage<T>): KafkaSendResult {
        return result
    }

    override suspend fun send(message: KafkaMessage<T>, topic: String): KafkaSendResult {
        return result
    }

    override fun send(messages: Collection<KafkaMessage<T>>): Flow<KafkaSendResult> {
        return listOf(result).asFlow()
    }

    override fun send(messages: Collection<KafkaMessage<T>>, topic: String): Flow<KafkaSendResult> {
        return listOf(result).asFlow()
    }

    override fun send(messages: Flow<KafkaMessage<T>>): Flow<KafkaSendResult> {
        return listOf(result).asFlow()
    }

    override fun send(messages: Flow<KafkaMessage<T>>, topic: String): Flow<KafkaSendResult> {
        return listOf(result).asFlow()
    }
}
