package com.rarible.flow.api.config

import com.nftco.flow.sdk.AsyncFlowAccessApi
import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.api.service.FlowSignatureService
import com.rarible.flow.core.config.AppProperties
import com.rarible.flow.core.converter.OrderToDtoConverter
import com.rarible.protocol.currency.api.client.CurrencyApiClientFactory
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import io.netty.handler.logging.LogLevel
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.logging.AdvancedByteBufFormat


@Configuration
@EnableConfigurationProperties(ApiProperties::class)
class Config(
    val appProperties: AppProperties,
    val apiProperties: ApiProperties
) {

    @Bean
    fun signatureService(api: AsyncFlowAccessApi): FlowSignatureService {
        return FlowSignatureService(
            appProperties.chainId,
            api
        )
    }

    @Bean
    fun api(): AsyncFlowAccessApi = Flow.newAsyncAccessApi(apiProperties.flowAccessUrl, apiProperties.flowAccessPort)

    @Bean
    fun pinataClient(): WebClient {
        return buildWebClient("PinataClient", "https://rarible.mypinata.cloud/ipfs")
    }

    private fun buildWebClient(loggerName: String, baseUrl: String): WebClient {
        val httpClient: HttpClient = HttpClient
            .create()
            .wiretap(
                loggerName,
                LogLevel.INFO,
                AdvancedByteBufFormat.TEXTUAL
            )

        return WebClient
            .builder()
            .baseUrl(baseUrl)
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .build()
    }


    @Bean
    fun matrixWorldClient(): WebClient {
        return WebClient.create("https://api.matrixworld.org/land/api/v1/land/metadata/estate/flow/")
    }

    @EventListener(ApplicationReadyEvent::class)
    fun configureFlow() {
        Flow.DEFAULT_ADDRESS_REGISTRY.apply {
            register("0xMOTOGPTOKEN", FlowAddress("0x01658d9b94068f3c"), FlowChainId.TESTNET)
            register("0xEVOLUTIONTOKEN", FlowAddress("0x01658d9b94068f3c"), FlowChainId.TESTNET)
            register("0xTOPSHOTTOKEN", FlowAddress("0x01658d9b94068f3c"), FlowChainId.TESTNET)
            register("0xRARIBLETOKEN", FlowAddress("0xebf4ae01d1284af8"), FlowChainId.TESTNET)
            register("0xTOPSHOTROYALTIES", FlowAddress("0xebf4ae01d1284af8"), FlowChainId.TESTNET)
            register("0xMUGENNFT", FlowAddress("0xebf4ae01d1284af8"), FlowChainId.TESTNET)
            register("0xCNNNFT", FlowAddress("0xebf4ae01d1284af8"), FlowChainId.TESTNET)
            register("0xMATRIXWORLDFLOWFEST", FlowAddress("0xe2f1b000e0203c1d"), FlowChainId.TESTNET)
            register("0xMATRIXWORLD", FlowAddress("0xe2f1b000e0203c1d"), FlowChainId.TESTNET)
            register("0xVERSUSART", FlowAddress("0x99ca04281098b33d"), FlowChainId.TESTNET)
            register("0xDISRUPTART", FlowAddress("0x439c2b49c0b2f62b"), FlowChainId.TESTNET)
            register("0xDISRUPTARTROYALTY", FlowAddress("0x439c2b49c0b2f62b"), FlowChainId.TESTNET)


            register("0xMOTOGPTOKEN", FlowAddress("0xa49cc0ee46c54bfb"), FlowChainId.MAINNET)
            register("0xEVOLUTIONTOKEN", FlowAddress("0xf4264ac8f3256818"), FlowChainId.MAINNET)
            register("0xTOPSHOTTOKEN", FlowAddress("0x0b2a3299cc857e29"), FlowChainId.MAINNET)
            register("0xRARIBLETOKEN", FlowAddress("0x01ab36aaf654a13e"), FlowChainId.MAINNET)
            register("0xTOPSHOTROYALTIES", FlowAddress("0xbd69b6abdfcf4539"), FlowChainId.MAINNET)
            register("0xMUGENNFT", FlowAddress("0x2cd46d41da4ce262"), FlowChainId.MAINNET)
            register("0xCNNNFT", FlowAddress("0x329feb3ab062d289"), FlowChainId.MAINNET)
            register("0xMATRIXWORLDFLOWFEST", FlowAddress("0x2d2750f240198f91"), FlowChainId.MAINNET)
            register("0xMATRIXWORLD", FlowAddress("0xebf4ae01d1284af8"), FlowChainId.MAINNET)
            register("0xVERSUSART", FlowAddress("0xd796ff17107bbff6"), FlowChainId.MAINNET)
            register("0xDISRUPTART", FlowAddress("0xcd946ef9b13804c6"), FlowChainId.MAINNET)
            register("0xDISRUPTARTROYALTY", FlowAddress("0x420f47f16a214100"), FlowChainId.MAINNET)
        }

        Flow.configureDefaults(chainId = appProperties.chainId)
    }

    @Bean
    fun currencyApi(factory: CurrencyApiClientFactory): CurrencyControllerApi {
        return factory.createCurrencyApiClient()
    }

    @Bean
    fun orderToDtoConverter(currencyApi: CurrencyControllerApi): OrderToDtoConverter {
        return OrderToDtoConverter(currencyApi)
    }
}
