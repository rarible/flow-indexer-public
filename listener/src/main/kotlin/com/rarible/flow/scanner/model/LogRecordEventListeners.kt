package com.rarible.flow.scanner.model

object LogRecordEventListeners {

    fun listenerId(env: String, listenerId: String): String = "${prefix(env)}.$listenerId.listener"

    private fun prefix(env: String): String = "$env.protocol.flow.scanner"
}
