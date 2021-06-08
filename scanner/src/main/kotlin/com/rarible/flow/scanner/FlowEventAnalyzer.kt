package com.rarible.flow.scanner

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.rarible.flow.scanner.model.EventMessage
import com.rarible.flow.scanner.model.FlowTransaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Created by TimochkinEA at 08.06.2021
 */
@Service
class FlowEventAnalyzer {

    private val log: Logger = LoggerFactory.getLogger(FlowEventAnalyzer::class.java)

    //TODO get from config
    private val contracts = listOf("1cd85950d20f05b2", "9a0766d93b6608b7", "0287aa0a33a82d7a", "9837bf0b0b963a4a", "e3750a9bc4137f3f", "f9a15cf06773248c")

    private val mapper = ObjectMapper()

    /**
     * Analysis of the transaction for events of interest
     */
    fun analyze(tx: FlowTransaction) {
        tx.events.forEach { event ->
            if (contracts.any { event.type.contains(it, true) }) {
                val data = mapper.readValue<EventMessage>(event.data)
                log.info("$data")
            }
        }
    }
}
