package com.rarible.flow.api.controller

import com.rarible.core.apm.withSpan
import com.rarible.flow.log.Log
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class InstrumentationFilter: WebFilter {
    private val logger by Log()

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val name = "${exchange.request.methodValue} ${exchange.request.path.pathWithinApplication().value()}"
        val labels = exchange.request.queryParams.toList()
        logger.trace("Instrumentation of HTTP request: {} with attributes {}", name, labels)
        return chain.filter(exchange).withSpan(
            name = name,
            type = "flow-api",
            labels = labels
        )
    }
}