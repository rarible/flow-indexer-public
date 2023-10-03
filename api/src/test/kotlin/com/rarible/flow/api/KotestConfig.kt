package com.rarible.flow.api

import io.kotest.core.config.AbstractProjectConfig

object KotestConfig : AbstractProjectConfig() {
    override val globalAssertSoftly: Boolean?
        get() = true

    // override fun extensions() = listOf(SpringExtension)
}
