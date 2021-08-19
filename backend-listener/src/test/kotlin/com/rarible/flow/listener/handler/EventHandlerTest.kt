package com.rarible.flow.listener.handler

import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.events.EventId
import com.rarible.flow.events.EventMessage
import com.rarible.flow.listener.handler.listeners.SmartContractEventHandler
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.onflow.sdk.FlowAddress
import java.time.LocalDateTime


internal class EventHandlerTest: FunSpec({

    val handledEvents = mutableListOf<Pair<String, TokenId>>()
    val eventHandler = EventHandler(
        mapOf<String, SmartContractEventHandler<*>>(
            "SomeShot.Withdraw" to object : SmartContractEventHandler<Unit> {
                override suspend fun handle(contract: String, tokenId: TokenId, fields: Map<String, Any?>, blockInfo: BlockInfo) {
                    handledEvents.add(
                        contract to tokenId
                    )
                }
            }
        )
    )

    beforeAny {
        handledEvents.clear()
    }

    test("should handle a event") {
        eventHandler.handle(
            EventMessage(
                EventId.of("A.877931736ee77123.SomeShot.Withdraw"),
                mapOf("id" to 6497086),
                LocalDateTime.parse("2021-07-27T16:55:47.778468127"),
                BlockInfo()
            )
        )

        handledEvents shouldHaveSize 1
        handledEvents[0].first shouldBe "SomeShot" //todo need strict format
        handledEvents[0].second shouldBe 6497086
    }

    test("should skip event") {
        eventHandler.handle(
            EventMessage(
                EventId.of("A.877931736ee77123.SomeShot.Withdraw"),
                mapOf("id" to null),
                LocalDateTime.parse("2021-07-27T16:55:47.778468127"),
                BlockInfo()
            )
        )

        handledEvents shouldHaveSize 0
    }

})
