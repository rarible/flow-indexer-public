package com.rarible.flow.scanner

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.rarible.flow.events.EventMessage
import com.rarible.flow.log.Log
import com.rarible.flow.scanner.config.ScannerProperties
import com.rarible.flow.scanner.model.FlowTransaction
import com.rarible.flow.scanner.model.RariEvent
import com.rarible.flow.scanner.repo.RariEventRepository
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * Created by TimochkinEA at 01.07.2021
 *
 * Use for replace "send to kafka" with "save to db"
 */
@Component
@Profile("no-kafka")
@Primary
class NoKafkaFlowEventAnalyzer(
    private val flowMapper: ObjectMapper,
    private val scannerProperties: ScannerProperties,
    private val eventRepository: RariEventRepository
): IFlowEventAnalyzer {

    private val log by Log()

    override fun analyze(tx: FlowTransaction) {
        log.info("analyze ...")
        val rariEvents = tx.events.filter{ flowEvent -> scannerProperties.trackedContracts.any { contract -> flowEvent.type.contains(contract, true) } }
            .mapIndexed { idx, filteredEvent ->
                val data = flowMapper.readValue<EventMessage>(filteredEvent.data)
                data.timestamp = filteredEvent.timestamp
                log.info("Catch event!")
                log.info("$data")
                RariEvent(
                    id = "${tx.id}.${idx}",
                    data = flowMapper.writeValueAsString(data)
                )

            }
        eventRepository.saveAll(rariEvents).subscribe()
    }
}
