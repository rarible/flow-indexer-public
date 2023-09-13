package com.rarible.flow.api

import ch.sbb.esta.openshift.gracefullshutdown.GracefulShutdownHook
import com.rarible.core.kafka.KafkaShutdownHook
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class FlowMarketplaceBackendApp

fun main(args: Array<String>) {
    val app = SpringApplication(FlowMarketplaceBackendApp::class.java)
    app.setRegisterShutdownHook(false)
    val context = app.run(*args)
    Runtime.getRuntime().addShutdownHook(Thread(KafkaShutdownHook(context, GracefulShutdownHook(context))))
}
