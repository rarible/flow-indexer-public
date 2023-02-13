package com.rarible.flow.scanner.model

object LogRecordEventListeners {

    fun listenerId(env: String, groupId: String): String = "${prefix(env)}.$groupId.listener"

    private fun prefix(env: String): String = "$env.protocol.flow.scanner"
}
