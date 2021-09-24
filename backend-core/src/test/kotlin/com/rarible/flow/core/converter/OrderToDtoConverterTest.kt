package com.rarible.flow.core.converter

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.FlowAssetFungible
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.flow.core.domain.OrderData
import com.rarible.flow.core.domain.Payout
import com.rarible.flow.core.repository.data
import com.rarible.protocol.dto.FlowAssetFungibleDto
import com.rarible.protocol.dto.FlowAssetNFTDto
import com.rarible.protocol.dto.FlowOrderDataDto
import com.rarible.protocol.dto.PayInfoDto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

internal class OrderToDtoConverterTest: FunSpec({

    test("should convert empty order data") {
        val orderData = OrderData(
            emptyList(), emptyList()
        )

        OrderToDtoConverter.convert(orderData) shouldBe FlowOrderDataDto(emptyList(), emptyList())
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

        OrderToDtoConverter.convert(orderData) should { od: FlowOrderDataDto ->
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
        OrderToDtoConverter.convert(
            FlowAssetNFT("A.B.C", 1.toBigDecimal(), 1337L)
        ) shouldBe FlowAssetNFTDto("A.B.C", 1.toBigDecimal(), 1337L.toBigInteger())
    }

    test("should convert fungible asset") {
        OrderToDtoConverter.convert(
            FlowAssetFungible("A.B.C", 10.25.toBigDecimal())
        ) shouldBe FlowAssetFungibleDto("A.B.C", 10.25.toBigDecimal())
    }

    test("should convert order without taker") {
        val order = data.createOrder()

        OrderToDtoConverter.convert(order) should { o ->
            o.id shouldBe order.id
            o.itemId shouldBe "0x0000000000000001:1"
            o.taker shouldBe null
            o.fill shouldBe 13.37.toBigDecimal()
        }
    }

    test("should convert order with taker") {
        val order = data.createOrder().copy(taker = FlowAddress("0x1337"))

        OrderToDtoConverter.convert(order) should { o ->
            o.id shouldBe order.id
            o.itemId shouldBe "0x0000000000000001:1"
            o.taker shouldBe "0x0000000000001337"
            o.fill shouldBe 13.37.toBigDecimal()
        }
    }

})
