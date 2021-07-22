package com.rarible.flow.api.controller

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import org.springframework.web.util.NestedServletException
import java.lang.UnsupportedOperationException

/**
 * Created by TimochkinEA at 21.07.2021
 */
@ControllerAdvice
class ErrorHandler: ResponseEntityExceptionHandler() {

    @ExceptionHandler
    fun handle(ex: Exception, request: WebRequest): ResponseEntity<Any> =
         ResponseEntity(
             mapOf(
                 "status" to HttpStatus.INTERNAL_SERVER_ERROR.value(),
                 "code" to "UNKNOWN", //TODO resolve error statuses
                 "message" to ex.message
             ),
             HttpHeaders(),
             HttpStatus.INTERNAL_SERVER_ERROR
         )
}
