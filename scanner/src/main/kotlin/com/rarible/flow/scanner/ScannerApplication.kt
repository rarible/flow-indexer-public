package com.rarible.flow.scanner

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@SpringBootApplication(exclude = [WebMvcAutoConfiguration::class])
@EnableMongoRepositories(basePackages = ["com.rarible.flow.scanner.repo"])
@EnableWebFlux
@EnableScheduling
@EnableWebSocketMessageBroker
class ScannerApplication : WebSocketMessageBrokerConfigurer, SchedulingConfigurer {

    @Bean
    fun scannerTaskExecutor(): Executor = Executors.newScheduledThreadPool(2)

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/topic").setAllowedOriginPatterns("*").withSockJS()
    }

    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        taskRegistrar.setScheduler(scannerTaskExecutor())
    }
}

fun main(args: Array<String>) {
    runApplication<ScannerApplication>(*args)
}
