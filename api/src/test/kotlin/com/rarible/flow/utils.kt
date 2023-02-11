package com.rarible.flow

import com.nftco.flow.sdk.FlowAddress
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils
import kotlin.random.Random

const val HEX_DIGITS = "0123456789ABCEDF"

fun randomHex(length: Int) = RandomStringUtils.random(length, HEX_DIGITS).lowercase()

fun randomAddress() = "0x${randomHex(16)}"

fun randomContract() = "A.${randomHex(16)}.Contract"

fun randomHash() = randomHex(64)

fun randomFlowAddress() = FlowAddress(randomAddress())

fun randomLong() = Random.Default.nextLong(0L, Long.MAX_VALUE)

fun randomRate() = Random.Default.nextDouble(0.0, 1.0)
