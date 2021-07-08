package com.rarible.flow.scanner

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.KafkaProducer
import com.rarible.core.kafka.KafkaSendResult
import com.rarible.flow.events.EventMessage
import com.rarible.flow.log.Log
import com.rarible.flow.scanner.model.RariEventMessageCaught
import kotlinx.coroutines.runBlocking
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * Created by TimochkinEA at 07.07.2021
 */
@Profile("!no-kafka")
@Component
class SendRariEventMessageToKafka(
    private val kafkaProducer: KafkaProducer<EventMessage>
): ApplicationListener<RariEventMessageCaught> {

    private val log by Log()

    override fun onApplicationEvent(event: RariEventMessageCaught) {
        runBlocking {
            val kafkaMessage = KafkaMessage(
                id = event.message.messageId,
                key = event.message.messageId,
                headers = emptyMap(),
                value = event.message.event
            )
            kafkaProducer.send(kafkaMessage).let { result ->
                when(result) {
                    is KafkaSendResult.Success -> {}
                    is KafkaSendResult.Fail -> log.warn("Failed to send to kafka", result.exception)
                }
            }
        }
    }
}
