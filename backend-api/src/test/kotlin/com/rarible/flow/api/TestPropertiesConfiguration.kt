package com.rarible.flow.api

import com.rarible.flow.core.converter.OrderToDtoConverter
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.currency.dto.BlockchainDto
import com.rarible.protocol.currency.dto.CurrencyRateDto
import io.mockk.every
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import reactor.core.publisher.Mono
import java.time.Instant

@TestConfiguration
class TestPropertiesConfiguration {

    @Bean
    @Primary
    fun currencyApi(): CurrencyControllerApi {
        return mockk {
            every {
                getCurrencyRate(eq(BlockchainDto.FLOW), any(), any())
            } returns Mono.just(
                CurrencyRateDto("FLOW", "USD", 10.toBigDecimal(), Instant.now())
            )
        }
    }

    @Bean
    @Primary
    fun orderConverter(currencyApi: CurrencyControllerApi): OrderToDtoConverter {
        return OrderToDtoConverter(currencyApi)
    }

}