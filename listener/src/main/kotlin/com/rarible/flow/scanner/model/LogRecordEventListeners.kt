package com.rarible.flow.scanner.model

object LogRecordEventListeners {

    fun listenerId(listenerId: String): String = "protocol.flow.scanner.$listenerId.listener"

}
