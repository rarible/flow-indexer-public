package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemHistory
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.events.EventId
import com.rarible.flow.events.EventMessage
import com.rarible.flow.listener.handler.EventHandler
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import reactor.core.publisher.Mono
import java.time.LocalDateTime

internal class TransferListenerTest: FunSpec({
    val listener = TransferListener(
        mockk() {
            every { save(any()) } answers {
                Mono.just(it.invocation.args[0] as ItemHistory)
            }
        },
        mockk() {
        }
    )

    val eventHandler = EventHandler(
        mapOf(
            TransferListener.ID to listener
        )
    )

    test("should handle transfer") {
        val event = EventMessage(
            EventId.of("A.fcfb23c627a63d40.CommonNFT.Transfer"),
            mapOf(
                "from" to "0xfcfb23c627a63d00",
                "to" to "0xfcfb23c627a63d40",
            ),
            LocalDateTime.parse("2021-07-29T05:59:58.425384445"),
            BlockInfo(
                "357157d9cb0bc433689a1f76ba0fc08083f9a47d3725f09e8f0d2cf64671ad6b",
                40172320,
                "469c76f0a6050c0ff0e5dcee1f8aa3d4244498ff26ce47aeab7e6e695c4d7811"
            )
        )

        eventHandler.handle(event)
    }
})
