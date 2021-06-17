package com.rarible.flow.listener

import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.flow.events.NftEvent
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FlowListenerApp(
    private val eventConsumerWorker: ConsumerWorker<NftEvent>
): CommandLineRunner {
    override fun run(vararg args: String?) {
        eventConsumerWorker.start()
    }
}

fun main(args: Array<String>) {
    runApplication<FlowListenerApp>(*args)
}