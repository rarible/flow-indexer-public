package com.rarible.flow.api.config

import com.netflix.graphql.dgs.client.MonoGraphQLClient
import com.netflix.graphql.dgs.client.WebClientGraphQLClient
import com.nftco.flow.sdk.AsyncFlowAccessApi
import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.impl.AsyncFlowAccessApiImpl
import com.rarible.flow.Contracts
import com.rarible.flow.api.service.FlowSignatureService
import com.rarible.flow.core.config.AppProperties
import com.rarible.flow.core.converter.OrderToDtoConverter
import com.rarible.protocol.currency.api.client.CurrencyApiClientFactory
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import io.grpc.ManagedChannelBuilder
import io.netty.handler.logging.LogLevel
import org.onflow.protobuf.access.AccessAPIGrpc
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

    @Suppress("PrivatePropertyName")
    private val DEFAULT_MESSAGE_SIZE: Int = 33554432 //32 Mb in bites

    @Bean
    fun signatureService(api: AsyncFlowAccessApi): FlowSignatureService {
        return FlowSignatureService(
            appProperties.chainId,
            api
        )
    }

    @Bean
    fun api(): AsyncFlowAccessApi {
        val channel = ManagedChannelBuilder.forAddress(apiProperties.flowAccessUrl, apiProperties.flowAccessPort)
            .maxInboundMessageSize(DEFAULT_MESSAGE_SIZE)
            .usePlaintext()
            .userAgent(Flow.DEFAULT_USER_AGENT)
            .build()
        return AsyncFlowAccessApiImpl(AccessAPIGrpc.newFutureStub(channel))
    }

    @Bean
    fun pinataClient(): WebClient {
        return buildWebClient("pinataClient", "https://rarible.mypinata.cloud/ipfs")
    }

    @Bean
    fun webClient(): WebClient {
        return WebClient.create()
    }

    private fun buildWebClient(loggerName: String, baseUrl: String): WebClient {
        val httpClient = HttpClient
            .create()
            .wiretap(loggerName, LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL)

        return WebClient
            .builder()
            .baseUrl(baseUrl)
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .build()
    }

    @Bean
    fun chainMonstersGraphQl(): WebClientGraphQLClient {
        return MonoGraphQLClient.createWithWebClient(
            buildWebClient("chainMonstersGraphQl", "https://europe-west3-chainmonstersmmo.cloudfunctions.net/graphql")
        )
    }

    @Bean
    fun matrixWorldClient(): WebClient {
        return buildWebClient(
            "matrixWorldClient",
            "https://api.matrixworld.org/land/api/v1/land/metadata/estate/flow/"
        )
    }

    @EventListener(ApplicationReadyEvent::class)
    fun configureFlow() {
        Contracts.values().forEach {
            it.register(Flow.DEFAULT_ADDRESS_REGISTRY)
        }
        Flow.DEFAULT_ADDRESS_REGISTRY.apply {
            register("0xTOPSHOTTOKEN", FlowAddress("0x01658d9b94068f3c"), FlowChainId.TESTNET)
            register("0xRARIBLETOKEN", FlowAddress("0xebf4ae01d1284af8"), FlowChainId.TESTNET)
            register("0xTOPSHOTROYALTIES", FlowAddress("0xebf4ae01d1284af8"), FlowChainId.TESTNET)
            register("0xMUGENNFT", FlowAddress("0xebf4ae01d1284af8"), FlowChainId.TESTNET)
            register("0xVERSUSART", FlowAddress("0x99ca04281098b33d"), FlowChainId.TESTNET)
            register("0xDISRUPTART", FlowAddress("0x439c2b49c0b2f62b"), FlowChainId.TESTNET)
            register("0xDISRUPTARTROYALTY", FlowAddress("0x439c2b49c0b2f62b"), FlowChainId.TESTNET)
            register("0xMETADATAVIEWS", FlowAddress("0x631e88ae7f1d7c20"), FlowChainId.TESTNET)

            register("0xTOPSHOTTOKEN", FlowAddress("0x0b2a3299cc857e29"), FlowChainId.MAINNET)
            register("0xRARIBLETOKEN", FlowAddress("0x01ab36aaf654a13e"), FlowChainId.MAINNET)
            register("0xTOPSHOTROYALTIES", FlowAddress("0xbd69b6abdfcf4539"), FlowChainId.MAINNET)
            register("0xMUGENNFT", FlowAddress("0x2cd46d41da4ce262"), FlowChainId.MAINNET)
            register("0xVERSUSART", FlowAddress("0xd796ff17107bbff6"), FlowChainId.MAINNET)
            register("0xDISRUPTART", FlowAddress("0xcd946ef9b13804c6"), FlowChainId.MAINNET)
            register("0xDISRUPTARTROYALTY", FlowAddress("0x420f47f16a214100"), FlowChainId.MAINNET)
            register("0xMETADATAVIEWS", FlowAddress("0x1d7e57aa55817448"), FlowChainId.MAINNET)
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
