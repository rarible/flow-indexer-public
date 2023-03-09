package com.rarible.flow.scanner.config

import com.github.cloudyrock.spring.v5.EnableMongock
import com.nftco.flow.sdk.*
import com.rarible.blockchain.scanner.flow.EnableFlowBlockchainScanner
import com.rarible.core.task.EnableRaribleTask
import com.rarible.flow.core.converter.OrderToDtoConverter
import com.rarible.flow.core.repository.BalanceRepository
import com.rarible.flow.core.repository.TaskItemHistoryRepository
import com.rarible.flow.scanner.service.balance.FlowBalanceService
import com.rarible.protocol.currency.api.client.CurrencyApiClientFactory
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

@Configuration
@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
@FlowPreview
@EnableMongock
@EnableRaribleTask
@EnableFlowBlockchainScanner
@EnableConfigurationProperties(FlowListenerProperties::class)
@EnableReactiveMongoAuditing
class Config(
    private val flowListenerProperties: FlowListenerProperties
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
    fun flowBalanceService(
        api: AsyncFlowAccessApi,
        balanceRepository: BalanceRepository
    ): FlowBalanceService {
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

        Flow.configureDefaults(chainId = flowListenerProperties.chainId)

        return FlowBalanceService(
            flowListenerProperties.chainId,
            api,
            balanceRepository
        )
    }

    @Bean
    fun asyncApi(): AsyncFlowAccessApi = Flow.newAsyncAccessApi(flowListenerProperties.flowAccessUrl, flowListenerProperties.flowAccessPort)

    @Bean
    fun syncApy(): FlowAccessApi = Flow.newAccessApi(flowListenerProperties.flowAccessUrl, flowListenerProperties.flowAccessPort)

    @Bean
    fun taskItemHistoryRepository(mongo: ReactiveMongoTemplate): TaskItemHistoryRepository {
        return TaskItemHistoryRepository(mongo)
    }
}
