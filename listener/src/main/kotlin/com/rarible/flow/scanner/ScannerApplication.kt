package com.rarible.flow.scanner

import com.rarible.flow.scanner.record.KafkaConsumerWorker
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ScannerApplication(
    private val kafkaConsumerWorkers: List<KafkaConsumerWorker<*>>
) : CommandLineRunner {
    override fun run(vararg args: String?) {
        kafkaConsumerWorkers.forEach { it.start() }
    }
}

fun main(args: Array<String>) {
    runApplication<ScannerApplication>(*args)
}
