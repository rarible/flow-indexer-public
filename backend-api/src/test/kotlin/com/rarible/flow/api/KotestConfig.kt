package com.rarible.flow.api

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.test.AssertionMode



object KotestConfig: AbstractProjectConfig() {
    override val globalAssertSoftly: Boolean?
        get() = true

    //override fun extensions() = listOf(SpringExtension)
}