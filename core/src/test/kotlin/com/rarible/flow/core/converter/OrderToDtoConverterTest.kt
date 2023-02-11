package com.rarible.flow.core.converter

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.data
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.currency.dto.CurrencyRateDto
import com.rarible.protocol.dto.*
import io.kotest.core.datatest.forAll
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

internal class OrderToDtoConverterTest: FunSpec({

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
            converter.convert(status) shouldBe when(status) {
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
            listOf(
                Payout(FlowAddress("0x01"), 7500.toBigDecimal()),
                Payout(FlowAddress("0x02"), 2500.toBigDecimal()),
            ),
            listOf(
                Payout(FlowAddress("0x01"), 75.toBigDecimal()),
                Payout(FlowAddress("0x02"), 25.toBigDecimal()),
            )
        )

        converter.convert(orderData) should { od: FlowOrderDataDto ->
            od.originalFees shouldContainAll listOf(
                PayInfoDto(FlowAddress("0x01").formatted, 75.toBigDecimal()),
                PayInfoDto(FlowAddress("0x02").formatted, 25.toBigDecimal())
            )

            od.payouts shouldContainAll listOf(
                PayInfoDto(FlowAddress("0x01").formatted, 7500.toBigDecimal()),
                PayInfoDto(FlowAddress("0x02").formatted, 2500.toBigDecimal())
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
            o.id shouldBe order.id
            o.itemId shouldBe "0x0000000000000001:1"
            o.taker shouldBe null
            o.fill shouldBe 13.37.toBigDecimal()
            o.makeStock shouldBe BigDecimal.ONE
        }
    }

    //TODO we should not put taker to our orders
    test("should convert order with taker") {
        val order = data.createOrder().copy(taker = FlowAddress("0x1337"))

        converter.convert(order) should { o ->
            o.id shouldBe order.id
            o.itemId shouldBe "0x0000000000000001:1"
            o.taker shouldBe null
            o.fill shouldBe 13.37.toBigDecimal()
            o.priceUsd shouldBe 100.toBigDecimal()
        }
    }

})
