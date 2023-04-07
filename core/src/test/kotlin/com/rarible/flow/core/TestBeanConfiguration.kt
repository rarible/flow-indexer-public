package com.rarible.flow.core

import com.rarible.blockchain.scanner.flow.service.SporkService
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class TestBeanConfiguration {
    @Bean
    fun mockkSporkService(): SporkService {
        return mockk()
    }
}
