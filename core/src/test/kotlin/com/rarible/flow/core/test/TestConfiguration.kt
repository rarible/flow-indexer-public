package com.rarible.flow.core.test

import com.rarible.blockchain.scanner.flow.service.SporkService
import io.mockk.mockk
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
@EnableAutoConfiguration
class TestConfiguration {

    @Bean
    fun mockkSporkService(): SporkService {
        return mockk()
    }
}
