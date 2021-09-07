package com.rarible.flow.listener

import com.rarible.flow.listener.handler.listeners.*
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired


@IntegrationTest
class ListenerContextLoadTest: BaseIntegrationTest() {

    @Autowired
    lateinit var handlers: Map<String, SmartContractEventHandler>



    @Test
    fun contextLoads() {
        protocolEventPublisher shouldNotBe null
    }

    @Test
    fun loadsEventHandlers() {
        handlers.size shouldNotBe 0
        handlers[DepositListener.ID] shouldNotBe null
        handlers[DestroyListener.ID] shouldNotBe null
        handlers[MintListener.ID] shouldNotBe null
        handlers[SaleOfferCompleteListener.ID] shouldNotBe null
        handlers[SaleOfferAvailable.ID] shouldNotBe null
    }
}
