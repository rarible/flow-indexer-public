package com.rarible.flow.scanner.model

interface AbstractEventType {
    val eventName: String

    fun full(contract: String): String {
        return "$contract.${eventName}"
    }
}