package com.rarible.flow.core.repository

import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils
import java.util.*
import kotlin.random.Random


object data {
    fun randomAddress() = "0x${RandomStringUtils.random(16, "0123456789ABCDEF")}".lowercase(Locale.ENGLISH)

    fun randomLong() = Random.Default.nextLong(0L, Long.MAX_VALUE)
}