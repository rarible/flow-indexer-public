package com.rarible.flow.scanner

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.KafkaProducer
import com.rarible.core.kafka.KafkaSendResult
import com.rarible.flow.events.EventMessage
import com.rarible.flow.log.Log
import com.rarible.flow.scanner.model.FlowEvent
import com.rarible.flow.scanner.model.FlowTransaction
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking

class FlowEventAnalyzer(
    private val kafkaProducer: KafkaProducer<EventMessage>,
    private val flowMapper: ObjectMapper,
    private val contracts: List<String>
) {

    /**
     * Analysis of the transaction for events of interest
     */
    fun analyze(tx: FlowTransaction) {
        val kafkaMessages = makeKafkaMessages(tx.id, tx.events)
        log.info("Sending {} events...", kafkaMessages.size)

        runBlocking {
            kafkaProducer.send(kafkaMessages).collect { result ->
                when(result) {
                    is KafkaSendResult.Success -> {}
                    is KafkaSendResult.Fail -> log.warn("Failed to send to kafka", result.exception)
                }
            }
        }
    }

    fun isEventTracked(event: FlowEvent) =
        contracts.any { contract -> event.type.contains(contract, true) }

    fun makeKafkaMessages(txId: String, events: List<FlowEvent>) = events.mapIndexed { eventIndex, event ->
        if (isEventTracked(event)) {
            val data = flowMapper.readValue<EventMessage>(event.data)
            data.timestamp = event.timestamp
            KafkaMessage(
                key = "$txId.$eventIndex", //todo check collisions
                value = data,
                headers = emptyMap(),
                id = "$txId.$eventIndex"
            )
        } else {
            null
        }
    }.filterNotNull()


    companion object {
        val log by Log()
    }
}
