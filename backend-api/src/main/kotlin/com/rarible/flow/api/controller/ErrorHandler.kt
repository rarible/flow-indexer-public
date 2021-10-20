package com.rarible.flow.api.controller

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ExceptionsHandlers {

    @ExceptionHandler(IncorrectTokenId::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun incorrectTokenId(ex: IncorrectTokenId): String {
        return ex.message
    }

    @ExceptionHandler(IncorrectAddress::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun incorrectFlowAddress(ex: IncorrectAddress): String {
        return ex.message
    }
}