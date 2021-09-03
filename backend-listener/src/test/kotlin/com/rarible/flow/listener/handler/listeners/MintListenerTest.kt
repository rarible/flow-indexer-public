package com.rarible.flow.listener.handler.listeners

import com.nftco.flow.sdk.FlowAddress
import com.rarible.core.kafka.KafkaSendResult
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.events.EventId
import com.rarible.flow.events.EventMessage
import com.rarible.flow.listener.config.ItemCollectionProperty
import com.rarible.flow.listener.config.ListenerProperties
import com.rarible.flow.listener.handler.EventHandler
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDateTime

internal class MintListenerTest: FunSpec({

    val itemRepository = mockk<ItemRepository>() {
        every { save(any()) } answers { Mono.just(it.invocation.args[0] as Item) }
        every { findById(any<ItemId>()) } returns Mono.empty()
    }

    val ownershipRepository = mockk<OwnershipRepository>() {
        every { save(any()) } answers { Mono.just(it.invocation.args[0] as Ownership) }
    }

    val itemHistoryRepository = mockk<ItemHistoryRepository>() {
        every { save(any()) } answers { Mono.just(it.invocation.args[0] as ItemHistory) }
    }

    val protocolEventPublisher = mockk<ProtocolEventPublisher>() {
        coEvery { onItemUpdate(any()) } returns KafkaSendResult.Success("1")
    }

    val listener = MintListener(
        itemRepository,
        ownershipRepository,
        protocolEventPublisher,
        itemHistoryRepository
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
                "id" to "12",
                "collection" to "A.fcfb23c627a63d40.CommonNFT.NFT",
                "creator" to "0xfcfb23c627a63d40",
                "metadata" to "url://",
                "royalties" to listOf(
                    mapOf("address" to "0x2ec081d566da0184", "fee" to "25.00000000"),
                    mapOf("address" to "0xe91e497115b9731b", "fee" to "5.00000000"),
                )
            ),
            LocalDateTime.parse("2021-07-29T05:59:58.425384445"),
            BlockInfo(
                "357157d9cb0bc433689a1f76ba0fc08083f9a47d3725f09e8f0d2cf64671ad6b",
                40172320,
                "469c76f0a6050c0ff0e5dcee1f8aa3d4244498ff26ce47aeab7e6e695c4d7811"
            )
        )

        eventHandler.handle(event)

        verify(exactly = 1) {
            itemRepository.save(withArg {
                it.contract shouldBe "A.fcfb23c627a63d40.CommonNFT.NFT"
                it.tokenId shouldBe 12L
                it.creator shouldBe FlowAddress("0xfcfb23c627a63d40")
                it.royalties shouldContainAll listOf(
                        Part(FlowAddress("0x2ec081d566da0184"), 25.0),
                        Part(FlowAddress("0xe91e497115b9731b"), 5.0),
                    )
                it.owner shouldBe FlowAddress("0xfcfb23c627a63d40")
                it.listed shouldBe false
                it.collection shouldBe "A.fcfb23c627a63d40.CommonNFT.NFT"
            })

            ownershipRepository.save(withArg {
                it.contract shouldBe "A.fcfb23c627a63d40.CommonNFT.NFT"
                it.tokenId shouldBe 12L
                it.creators shouldContain Payout(FlowAddress("0xfcfb23c627a63d40"), BigDecimal.ONE)
            })
        }
    }


})
