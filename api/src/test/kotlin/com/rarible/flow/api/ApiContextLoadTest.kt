package com.rarible.flow.api

import com.rarible.flow.api.config.Config
import com.rarible.flow.core.config.CoreConfig
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import

@IntegrationTest
@Import(Config::class, CoreConfig::class)
class ApiContextLoadTest : BaseIntegrationTest() {

    @Test
    fun contextLoads() {}
}
