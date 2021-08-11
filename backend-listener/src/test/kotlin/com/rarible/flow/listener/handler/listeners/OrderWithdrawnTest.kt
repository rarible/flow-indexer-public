package com.rarible.flow.listener.handler.listeners

import com.rarible.core.kafka.KafkaSendResult
import com.rarible.flow.core.domain.*
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.events.EventId
import com.rarible.flow.events.EventMessage
import com.rarible.flow.listener.createItem
import com.rarible.flow.listener.handler.EventHandler
import io.kotest.core.spec.style.FunSpec
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.bson.types.ObjectId
import org.onflow.sdk.FlowAddress
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDateTime

internal class OrderWithdrawnTest: FunSpec({

    val item = createItem()
    val order = Order(
        1L,
        item.id,
        FlowAddress("0x1000"),
        null,
        FlowAssetNFT(item.contract, 1.toBigDecimal(), item.tokenId),
        null,
        1.toBigDecimal(),
        item.id.toString(),
        buyerFee = BigDecimal.ZERO,
        sellerFee = BigDecimal.ZERO,
        data = OrderData(emptyList(), emptyList()),
        collection = "ABC"
    )

    val listener = OrderWithdrawn(
        mockk("orderRepository") {
            every { deleteByItemId(any()) } returns Mono.just(order)
            every { findByItemId(any<ItemId>()) } returns Mono.just(order)
            every { findById(any<Long>()) } returns Mono.just(order)
            every { save(any()) } returns Mono.just(order)
        },


        mockk("itemRepository") {
            coEvery { unlist(any()) } returns mockk("unlistResultMock")
            coEvery { byId(any()) } returns item
        },

        mockk("protocolEventPublisher") {
            coEvery {
                onUpdate(any<Order>())
            } returns KafkaSendResult.Success("1")
        },

        mockk {
            every { save(any()) } answers {
                Mono.just(it.invocation.args[0] as ItemHistory)
            }
        }

        )

    val eventHandler = EventHandler(
        mapOf(
            OrderWithdrawn.ID to listener
        )
    )

    test("should handle order withdrawn") {
        val event = EventMessage(
            EventId.of("A.fcfb23c627a63d40.RegularSaleOrder.OrderWithdrawn"),
            mapOf(
                "id" to 12
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