package com.rarible.flow.core.repository

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.*
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random


object data {
    fun randomAddress() = "0x${RandomStringUtils.random(16, "0123456789ABCDEF")}".lowercase(Locale.ENGLISH)

    fun randomLong() = Random.Default.nextLong(0L, Long.MAX_VALUE)

    fun createOrder(id: Long = randomLong()) = Order(
        id,
        ItemId(FlowAddress("0x01").formatted, 1),
        FlowAddress("0x1000"),
        null,
        FlowAssetNFT("0x01", 1.toBigDecimal(), 1),
        FlowAssetFungible("FLOW", BigDecimal.TEN),
        1.toBigDecimal(),
//        ItemId("0x01", 1).toString(),
        data = OrderData(emptyList(), emptyList()),
        collection = "ABC",
        fill = 13.37.toBigDecimal(),
        lastUpdatedAt = LocalDateTime.now(),
        createdAt = LocalDateTime.now(),
        makeStock = BigInteger.TEN
    )

    fun createSellActivity() = FlowNftOrderActivitySell(
        price = BigDecimal.ONE,
        priceUsd = BigDecimal.ONE,
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
        hash = UUID.randomUUID().toString()
    )
}
