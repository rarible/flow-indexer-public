package com.rarible.flow.scanner

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(exclude = [WebMvcAutoConfiguration::class])
@EnableMongoRepositories(basePackages = ["com.rarible.flow.scanner.repo"])
@EnableScheduling
class ScannerApplication {

    private val log: Logger = LoggerFactory.getLogger(ScannerApplication::class.java)

    @KafkaListener(topics = ["RariEvent"], groupId = "rari-flow-scaner")
    fun cons(@Payload message: String) {
        log.info("Receive Rari Event!")
        log.info(message)
    }
}


fun main(args: Array<String>) {
    runApplication<ScannerApplication>(*args)
}
