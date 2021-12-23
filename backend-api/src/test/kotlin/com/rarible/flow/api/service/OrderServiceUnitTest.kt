package com.rarible.flow.api.service

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.api.createOrder
import com.rarible.flow.core.domain.FlowAssetFungible
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.repository.OrderRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import reactor.core.publisher.Flux
import java.math.BigDecimal

internal class OrderServiceUnitTest: FunSpec({

    test("should return sell orders currencies") {
        val repo = mockk<OrderRepository>() {
            every { findAllByMake(any(), any()) } returns Flux.fromIterable(
                listOf(
                    createOrder(),
                    createOrder().copy(take = FlowAssetFungible("FUSD", BigDecimal.TEN)),
                )
            )
        }
        OrderService(repo).sellCurrenciesByItemId(
            ItemId(FlowAddress("0x01").formatted, 1)
        ).toList() shouldContainAll listOf(
            FlowAssetFungible("FLOW", BigDecimal.ZERO),
            FlowAssetFungible("FUSD", BigDecimal.ZERO),
        )
    }

    test("should return sell orders currencies - empty") {
        val repo = mockk<OrderRepository>() {
            every { findAllByMake(any(), any()) } returns Flux.empty()
        }
        OrderService(repo).sellCurrenciesByItemId(
            ItemId(FlowAddress("0x01").formatted, 1)
        ).toList() shouldHaveSize 0
    }

    test("should return bid orders currencies") {
        val repo = mockk<OrderRepository>() {
            every { findAllByTake(any(), any()) } returns Flux.fromIterable(
                listOf(
                    createOrder().let {
                       it.copy(make = it.take, take = it.make)
                    },
                    createOrder().copy(make = FlowAssetFungible("FUSD", BigDecimal.TEN)),
                )
            )
        }
        OrderService(repo).bidCurrenciesByItemId(
            ItemId(FlowAddress("0x01").formatted, 1)
        ).toList() shouldContainAll listOf(
            FlowAssetFungible("FLOW", BigDecimal.ZERO),
            FlowAssetFungible("FUSD", BigDecimal.ZERO),
        )
    }

    test("should return bid orders currencies - empty") {
        val repo = mockk<OrderRepository>() {
            every { findAllByTake(any(), any()) } returns Flux.empty()
        }
        OrderService(repo).bidCurrenciesByItemId(
            ItemId(FlowAddress("0x01").formatted, 1)
        ).toList() shouldHaveSize 0
    }
})