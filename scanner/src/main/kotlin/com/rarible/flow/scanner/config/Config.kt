package com.rarible.flow.scanner.config

import com.nftco.flow.sdk.AsyncFlowAccessApi
import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.EnableFlowBlockchainScanner
import com.rarible.flow.core.converter.OrderToDtoConverter
import com.rarible.flow.scanner.service.balance.FlowBalanceService
import com.rarible.protocol.currency.api.client.CurrencyApiClientFactory
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import io.mongock.runner.springboot.EnableMongock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
@FlowPreview
@EnableMongock
@EnableFlowBlockchainScanner
@EnableConfigurationProperties(FlowApiProperties::class)
class Config(
    val flowApiProperties: FlowApiProperties
) {
    @Bean
    fun currencyApi(factory: CurrencyApiClientFactory): CurrencyControllerApi {
        return factory.createCurrencyApiClient()
    }

    @Bean
    fun orderToDtoConverter(currencyApi: CurrencyControllerApi): OrderToDtoConverter {
        return OrderToDtoConverter(currencyApi)
    }

    @Bean
    fun flowBalanceService(api: AsyncFlowAccessApi): FlowBalanceService {
        Flow.DEFAULT_ADDRESS_REGISTRY.apply {
            FlowChainId.TESTNET.let { testnet ->
                register("0xFUNGIBLETOKEN", FlowAddress("0x9a0766d93b6608b7"), testnet)
                register("0xFLOWTOKEN", FlowAddress("0x7e60df042a9c0868"), testnet)
                register("0xFUSDTOKEN", FlowAddress("0xe223d8a629e49c68"), testnet)
            }

            FlowChainId.MAINNET.let { mainnet ->
                register("0xFUNGIBLETOKEN", FlowAddress("0xf233dcee88fe0abe"), mainnet)
                register("0xFLOWTOKEN", FlowAddress("0x1654653399040a61"),  mainnet)
                register("0xFUSDTOKEN", FlowAddress("0x3c5959b568896393"), mainnet)
            }


        }

        Flow.configureDefaults(chainId = flowApiProperties.chainId)

        return FlowBalanceService(
            flowApiProperties.chainId,
            api
        )
    }

    @Bean
    fun api(): AsyncFlowAccessApi = Flow.newAsyncAccessApi(flowApiProperties.flowAccessUrl, flowApiProperties.flowAccessPort)
}
