package com.rarible.flow

import com.nftco.flow.sdk.FlowAddress
import java.util.Locale
import kotlin.random.Random
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils

fun randomAddress() = "0x${RandomStringUtils.random(16, "0123456789ABCDEF")}".lowercase(Locale.ENGLISH)

fun randomFlowAddress() = FlowAddress(randomAddress())

fun randomLong() = Random.Default.nextLong(0L, Long.MAX_VALUE)

fun randomRate() = Random.Default.nextDouble(0.0, 1.0)
