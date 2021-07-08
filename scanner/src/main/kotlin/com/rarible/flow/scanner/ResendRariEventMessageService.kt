package com.rarible.flow.scanner

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.KafkaProducer
import com.rarible.core.kafka.KafkaSendResult
import com.rarible.flow.events.EventMessage
import com.rarible.flow.log.Log
import com.rarible.flow.scanner.repo.RariEventMessageRepository
import kotlinx.coroutines.runBlocking
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * Created by TimochkinEA at 07.07.2021
 */
@Service
@Profile("!no-kafka")
class ResendRariEventMessageService(
    private val rariEventMessageRepository: RariEventMessageRepository,
    private val kafkaProducer: KafkaProducer<EventMessage>
) {

    private val log by Log()

    fun resendToKafka(date: LocalDateTime) {
        rariEventMessageRepository.afterDate(date).subscribe {
            runBlocking {
                val kafkaMessage = KafkaMessage(
                    id = it.messageId,
                    key = it.messageId,
                    headers = emptyMap(),
                    value = it.event
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
}
