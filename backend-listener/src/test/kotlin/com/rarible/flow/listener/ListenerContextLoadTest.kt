package com.rarible.flow.listener

import com.rarible.flow.listener.handler.listeners.MintListener
import com.rarible.flow.listener.handler.listeners.SmartContractEventHandler
import io.kotest.matchers.shouldNot
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired


@IntegrationTest
class ListenerContextLoadTest: BaseIntegrationTest() {

    @Autowired
    lateinit var handlers: Map<String, SmartContractEventHandler<*>>

    @Test
    fun contextLoads() {}

    @Test
    fun loadsEventHandlers() {
        handlers.size shouldNotBe 0
        handlers[MintListener.ID] shouldNotBe null
    }
}
