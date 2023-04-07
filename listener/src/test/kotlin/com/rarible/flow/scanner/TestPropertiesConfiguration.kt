package com.rarible.flow.scanner

import com.rarible.blockchain.scanner.flow.service.SporkService
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class TestPropertiesConfiguration {
    @Bean
    fun mockkSporkService(): SporkService {
        return mockk()
    }

}