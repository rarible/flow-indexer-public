package com.rarible.flow.scanner

import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ScannerApplication(
    private val kafkaConsumerWorkers: List<RaribleKafkaConsumerWorker<*>>
) : CommandLineRunner {
    override fun run(vararg args: String?) {
        kafkaConsumerWorkers.forEach { it.start() }
    }
}

fun main(args: Array<String>) {
    runApplication<ScannerApplication>(*args)
}
