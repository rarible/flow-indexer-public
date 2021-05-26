package com.rarible.flow.scanner

import com.rarible.flow.scanner.model.FlowTransaction
import com.rarible.flow.scanner.repo.FlowTransactionRepository
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.function.server.RequestPredicates.GET
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions.route
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
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
