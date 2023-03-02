package com.rarible.flow.scanner.service

import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.currency.dto.BlockchainDto
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant

@Component
class CurrencyService(
    private val currencyApi: CurrencyControllerApi
) {
    suspend fun getUsdRate(contract: String, at: Instant): BigDecimal? {
        return try {
            currencyApi.getCurrencyRate(BlockchainDto.FLOW, contract, at.toEpochMilli()).awaitSingle().rate
        } catch (e: Exception) {
            logger.warn("Unable to fetch USD price rate from currency api: ${e.message}", e)
            null
        }
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(CurrencyService::class.java)
    }
}