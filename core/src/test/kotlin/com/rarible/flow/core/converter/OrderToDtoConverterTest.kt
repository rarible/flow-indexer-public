package com.rarible.flow.core.converter

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.FlowAssetFungible
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.flow.core.domain.OrderData
import com.rarible.flow.core.domain.OrderStatus
import com.rarible.flow.core.domain.Payout
import com.rarible.flow.core.repository.data
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.currency.dto.CurrencyRateDto
import com.rarible.protocol.dto.FlowAssetFungibleDto
import com.rarible.protocol.dto.FlowAssetNFTDto
import com.rarible.protocol.dto.FlowOrderDataDto
import com.rarible.protocol.dto.FlowOrderStatusDto
import com.rarible.protocol.dto.PayInfoDto
import io.kotest.core.datatest.forAll
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.Instant

internal class OrderToDtoConverterTest : FunSpec({

    val currencyApi = mockk<CurrencyControllerApi>() {
        every { getCurrencyRate(any(), any(), any()) } returns Mono.just(
            CurrencyRateDto("FLOW", "USD", 10.toBigDecimal(), Instant.now())
        )
    }

    val converter = OrderToDtoConverter(currencyApi)

    test("should convert empty order data") {
        val orderData = OrderData(
            emptyList(), emptyList()
        )

        converter.convert(orderData) shouldBe FlowOrderDataDto(emptyList(), emptyList())
    }

    test("should convert status") {
        forAll(
            *OrderStatus.values()
        ) { status ->
            converter.convert(status) shouldBe when (status) {
                OrderStatus.ACTIVE -> FlowOrderStatusDto.ACTIVE
                OrderStatus.FILLED -> FlowOrderStatusDto.FILLED
                OrderStatus.HISTORICAL -> FlowOrderStatusDto.HISTORICAL
                OrderStatus.INACTIVE -> FlowOrderStatusDto.INACTIVE
                OrderStatus.CANCELLED -> FlowOrderStatusDto.CANCELLED
            }
        }
    }

    test("should convert filled order data") {
        val orderData = OrderData(
            payouts = listOf(
                Payout(FlowAddress("0x01"), 0.75.toBigDecimal()),
                Payout(FlowAddress("0x02"), 0.25.toBigDecimal()),
            ),
            originalFees = listOf(
                Payout(FlowAddress("0x01"), 0.075.toBigDecimal()),
                Payout(FlowAddress("0x02"), 0.025.toBigDecimal()),
            )
        )

        val converted = converter.convert(orderData)

        converted should { od: FlowOrderDataDto ->
            od.payouts shouldContainAll listOf(
                PayInfoDto(FlowAddress("0x01").formatted, BigDecimal("7500.00")),
                PayInfoDto(FlowAddress("0x02").formatted, BigDecimal("2500.00"))
            )
            od.originalFees shouldContainAll listOf(
                PayInfoDto(FlowAddress("0x01").formatted, BigDecimal("750.000")),
                PayInfoDto(FlowAddress("0x02").formatted, BigDecimal("250.000"))
            )
        }
    }

    test("should convert NFT asset") {
        FlowAssetConverter.convert(
            FlowAssetNFT("A.B.C", 1.toBigDecimal(), 1337L)
        ) shouldBe FlowAssetNFTDto("A.B.C", 1.toBigDecimal(), 1337L.toBigInteger())
    }

    test("should convert fungible asset") {
        FlowAssetConverter.convert(
            FlowAssetFungible("A.B.C", 10.25.toBigDecimal())
        ) shouldBe FlowAssetFungibleDto("A.B.C", 10.25.toBigDecimal())
    }

    test("should convert order without taker") {
        val order = data.createOrder()

        converter.convert(order) should { o ->
            o.id shouldBe order.id.toLong()
            o.itemId shouldBe "0x0000000000000001:1"
            o.taker shouldBe null
            o.fill shouldBe 13.37.toBigDecimal()
            o.makeStock shouldBe BigDecimal.ONE
        }
    }

    // TODO we should not put taker to our orders
    test("should convert order with taker") {
        val order = data.createOrder().copy(taker = FlowAddress("0x1337"))

        converter.convert(order) should { o ->
            o.id shouldBe order.id.toLong()
            o.itemId shouldBe "0x0000000000000001:1"
            o.taker shouldBe null
            o.fill shouldBe 13.37.toBigDecimal()
            o.priceUsd shouldBe 100.toBigDecimal()
        }
    }
})
