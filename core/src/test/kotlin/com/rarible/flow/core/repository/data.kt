package com.rarible.flow.core.repository

import com.nftco.flow.sdk.FlowAddress
import com.rarible.core.test.data.randomString
import com.rarible.flow.core.domain.FlowAssetFungible
import com.rarible.flow.core.domain.FlowAssetNFT
import com.rarible.flow.core.domain.FlowNftOrderActivitySell
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.LegacyOrder
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.OrderActivityMatchSide
import com.rarible.flow.core.domain.OrderData
import com.rarible.flow.core.domain.OrderType
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

object data {
    fun randomAddress() = "0x${RandomStringUtils.random(16, "0123456789ABCDEF")}".lowercase(Locale.ENGLISH)

    fun randomLong() = Random.Default.nextLong(0L, Long.MAX_VALUE)

    @Deprecated("Delete after the migration")
    fun createLegacyOrder(id: Long = randomLong()) = LegacyOrder(
        id = id,
        itemId = ItemId(FlowAddress("0x01").formatted, 1),
        maker = FlowAddress("0x1000"),
        taker = null,
        make = FlowAssetNFT("0x01", 1.toBigDecimal(), 1),
        take = FlowAssetFungible("FLOW", BigDecimal.TEN),
        amount = BigDecimal.TEN,
        type = OrderType.LIST,
        data = OrderData(emptyList(), emptyList()),
        collection = "ABC",
        fill = 13.37.toBigDecimal(),
        lastUpdatedAt = LocalDateTime.now(),
        createdAt = LocalDateTime.now(),
        makeStock = BigDecimal.ONE
    )

    fun createOrder(id: String = randomString()) = Order(
        id = id,
        itemId = ItemId(FlowAddress("0x01").formatted, 1),
        maker = FlowAddress("0x1000"),
        taker = null,
        make = FlowAssetNFT("0x01", 1.toBigDecimal(), 1),
        take = FlowAssetFungible("FLOW", BigDecimal.TEN),
        amount = BigDecimal.TEN,
        type = OrderType.LIST,
        data = OrderData(emptyList(), emptyList()),
        collection = "ABC",
        fill = 13.37.toBigDecimal(),
        lastUpdatedAt = LocalDateTime.now(),
        createdAt = LocalDateTime.now(),
        makeStock = BigDecimal.ONE
    )

    fun createSellActivity() = FlowNftOrderActivitySell(
        price = BigDecimal.ONE,
        priceUsd = 1.1.toBigDecimal(),
        tokenId = 1,
        left = OrderActivityMatchSide(
            FlowAddress("0x01").formatted,
            FlowAssetNFT("c1", BigDecimal.ONE, 1)
        ),
        right = OrderActivityMatchSide(
            FlowAddress("0x02").formatted,
            FlowAssetFungible("flow", BigDecimal.ONE)
        ),
        contract = "c1",
        timestamp = Instant.now(),
        hash = UUID.randomUUID().toString(),
        payments = emptyList()
    )
}
