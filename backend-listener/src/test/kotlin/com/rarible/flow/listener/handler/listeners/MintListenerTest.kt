package com.rarible.flow.listener.handler.listeners

import com.rarible.core.kafka.KafkaSendResult
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.events.EventId
import com.rarible.flow.events.EventMessage
import com.rarible.flow.listener.config.ItemCollectionProperty
import com.rarible.flow.listener.config.ListenerProperties
import com.rarible.flow.listener.handler.EventHandler
import io.kotest.core.spec.style.FunSpec
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import reactor.core.publisher.Mono
import java.time.LocalDateTime

internal class MintListenerTest: FunSpec({

    val listener = MintListener(
        mockk() {
            every { save(any()) } returns Mono.just(mockk())
            every { findById(any<ItemId>()) } returns Mono.empty()
        },

        mockk() {
            every { save(any()) } returns Mono.just(mockk())
        },

        mockk() {
            every { save(any()) } returns Mono.just(mockk())
        },

        mockk() {
            coEvery { onItemUpdate(any()) } returns KafkaSendResult.Success("1")
        },

        mockk() {
            every { save(any()) } returns Mono.just(mockk())
        },
        ListenerProperties(kafkaReplicaSet = "kafka", environment = "test", defaultItemCollection = ItemCollectionProperty(id = "ID", owner = "0x01", name = "TestCollection", symbol = "TC"))
    )

    val eventHandler = EventHandler(
        mapOf(
            "CommonNFT.Mint" to listener
        )
    )

    test("should handle mint") {

        val event = EventMessage(
            EventId.of("A.fcfb23c627a63d40.CommonNFT.Mint"),
            mapOf(
                "id" to 12,
                "collection" to "A.fcfb23c627a63d40.CommonNFT.NFT",
                "creator" to "0xfcfb23c627a63d40",
                "metadata" to "url://",
                "royalties" to null
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
