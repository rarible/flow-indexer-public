package com.rarible.flow.api.controller

import com.rarible.flow.log.Log
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

/**
 * Created by TimochkinEA at 21.07.2021
 */
@ControllerAdvice
class ErrorHandler: ResponseEntityExceptionHandler() {

    private val log by Log()

    @ExceptionHandler
    fun handle(ex: Exception): ResponseEntity<Any> {
        log.error(ex.message, ex)
        return ResponseEntity(
            mapOf(
                "status" to HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "code" to "UNKNOWN", //TODO resolve error statuses
                "message" to ex.message
            ),
            HttpHeaders(),
            HttpStatus.INTERNAL_SERVER_ERROR
        )
    }
}
