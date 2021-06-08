package com.rarible.flow.scanner

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [WebMvcAutoConfiguration::class])
class ScannerApplication


fun main(args: Array<String>) {
    runApplication<ScannerApplication>(*args)
}
