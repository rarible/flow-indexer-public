package com.rarible.flow.scanner

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(exclude = [WebMvcAutoConfiguration::class])
@EnableMongoRepositories(basePackages = ["com.rarible.flow.scanner.repo"])
@EnableScheduling
class ScannerApplication


fun main(args: Array<String>) {
    runApplication<ScannerApplication>(*args)
}
