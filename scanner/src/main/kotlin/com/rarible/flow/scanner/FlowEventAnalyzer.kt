package com.rarible.flow.scanner

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.flow.events.EventMessage
import com.rarible.flow.scanner.model.FlowTransaction
import com.rarible.flow.log.Log
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service

/**
 * Created by TimochkinEA at 08.06.2021
 */
@Service
class FlowEventAnalyzer(
    private val kafkaProducer: RaribleKafkaProducer<EventMessage>,
    private val flowMapper: ObjectMapper
) {

    //TODO get from config
    private val contracts = listOf("1cd85950d20f05b2", "9a0766d93b6608b7", "0287aa0a33a82d7a", "9837bf0b0b963a4a", "e3750a9bc4137f3f", "f9a15cf06773248c")


    /**
     * Analysis of the transaction for events of interest
     */
    fun analyze(tx: FlowTransaction) {
        val kafkaMessages: List<KafkaMessage<EventMessage>> = tx.events.mapIndexed { eventIndex, event ->
            if (contracts.any { event.type.contains(it, true) }) {
                val data = flowMapper.readValue<EventMessage>(event.data)
                data.timestamp = event.timestamp
                KafkaMessage(
                    key = "${tx.id}.$eventIndex", //todo check collisions
                    value = data,
                    headers = emptyMap(),
                    id = "${tx.id}.$eventIndex"
                )
            } else {
                null
            }
        }.filterNotNull()

        log.info("Sending {} events...", kafkaMessages.size)
        log.debug("Messages to send: {}", kafkaMessages)

        runBlocking {
            kafkaProducer.send(kafkaMessages)
        }
    }

    companion object {
        val log by Log()
    }
}
