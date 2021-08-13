package com.rarible.flow.api.controller

import org.springframework.http.ResponseEntity

fun <T> T?.okOr404(): ResponseEntity<T> = if (this == null) {
    ResponseEntity.status(404).build()
} else {
    ResponseEntity.ok(this)
}