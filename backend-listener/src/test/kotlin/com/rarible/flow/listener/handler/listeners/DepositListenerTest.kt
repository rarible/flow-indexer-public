package com.rarible.flow.listener.handler.listeners

import com.rarible.core.kafka.KafkaSendResult
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.events.EventId
import com.rarible.flow.events.EventMessage
import com.rarible.flow.listener.createItem
import com.rarible.flow.listener.handler.EventHandler
import io.kotest.core.spec.style.FunSpec
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

internal class DepositListenerTest: FunSpec({

    val item = createItem()

    val listener = DepositListener(
        mockk() {
            every { save(any()) } returns Mono.just(item)
            every { findById(any<ItemId>()) } returns Mono.just(item)
        },

        mockk() {
            val ownerships = listOf(Ownership(item.contract, item.tokenId, item.owner, item.date))
            every { findAllByContractAndTokenId(any(), any()) } returns
                    Flux.fromIterable(ownerships)
            every { saveAll(any<Iterable<Ownership>>()) } returns Flux.fromIterable(ownerships)
        },

        mockk() {
            coEvery {
                onUpdate(any())
            } returns KafkaSendResult.Success("1")
        }
    )

    val eventHandler = EventHandler(
        mapOf(
            DepositListener.ID to listener
        )
    )

    test("should handle deposit") {
        val event = EventMessage(
            EventId.of("A.fcfb23c627a63d40.CommonNFT.Deposit"),
            mapOf(
                "id" to 12,
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