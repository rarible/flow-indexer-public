package com.rarible.flow.scanner

import com.rarible.core.kafka.KafkaShutdownHook
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class ScannerApplication(
    private val kafkaConsumerWorkers: List<RaribleKafkaConsumerWorker<*>>
) : CommandLineRunner {
    override fun run(vararg args: String?) {
        kafkaConsumerWorkers.forEach { it.start() }
    }
}

fun main(args: Array<String>) {
    val app = SpringApplication(ScannerApplication::class.java)
    app.setRegisterShutdownHook(false)
    val context = app.run(*args)
    Runtime.getRuntime().addShutdownHook(Thread(KafkaShutdownHook(context, context::close)))
}
