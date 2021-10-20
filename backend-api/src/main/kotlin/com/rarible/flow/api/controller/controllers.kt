package com.rarible.flow.api.controller

import com.nftco.flow.sdk.FlowAddress
import org.springframework.http.ResponseEntity

class IncorrectTokenId(override val message: String, override val cause: Throwable?): Exception(message, cause)

class IncorrectAddress(override val message: String, override val cause: Throwable?): Exception(message, cause)

fun <T> T?.okOr404IfNull(): ResponseEntity<T> = if (this == null) {
    ResponseEntity.status(404).build()
} else {
    ResponseEntity.ok(this)
}

fun String.tokenId(): Long {
    try {
        return this.toLong()
    } catch (e: NumberFormatException) {
        throw IncorrectTokenId("Could not convert $this to tokenId", e)
    }
}

fun String?.flowAddress(): FlowAddress? {
    try {
        return if(this == null) {
            null
        } else {
            FlowAddress(this)
        }
    } catch (ex: IllegalArgumentException) {
        throw IncorrectAddress("Could not convert $this to FlowAddress", ex)
    }
}
