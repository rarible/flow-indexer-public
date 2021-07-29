package com.rarible.flow

import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils
import java.util.*

fun randomAddress() = "0x${RandomStringUtils.random(16, "0123456789ABCDEF")}".lowercase(Locale.ENGLISH)
