package com.rarible.flow

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FlowMarketplaceBackendApp

fun main(args: Array<String>) {
    runApplication<FlowMarketplaceBackendApp>(*args)
}
