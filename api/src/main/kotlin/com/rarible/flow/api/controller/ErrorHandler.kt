package com.rarible.flow.api.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rarible.flow.core.util.Log
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

data class HttpError(val status: Int, val code: String = "UNKNOWN", val message: String?)

@Configuration
@Order(-2)
class ErrorHandler: ErrorWebExceptionHandler {

    private val log by Log()

    private val objectMapper = jacksonObjectMapper()

    override fun handle(exchange: ServerWebExchange, ex: Throwable): Mono<Void> {
        log.error(ex.message, ex)
        val error = when (ex) {
            is IncorrectTokenId, is IncorrectAddress, is IncorrectItemId -> HttpError(
                status = HttpStatus.BAD_REQUEST.value(),
                message = ex.message
            )
            is ResponseStatusException -> HttpError(status = ex.rawStatusCode, message = ex.reason)
            else -> HttpError(status = HttpStatus.INTERNAL_SERVER_ERROR.value(), message = ex.message)
        }

        val bufferFactory = exchange.response.bufferFactory()
        val buffer = bufferFactory.wrap(objectMapper.writeValueAsBytes(error))
        exchange.response.headers.contentType = MediaType.APPLICATION_JSON
        exchange.response.statusCode = HttpStatus.valueOf(error.status)
        return exchange.response.writeWith(buffer.toMono())
    }
}
