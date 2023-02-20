package com.rarible.flow.api.config

import com.netflix.graphql.dgs.client.MonoGraphQLClient
import com.netflix.graphql.dgs.client.WebClientGraphQLClient
import com.nftco.flow.sdk.AsyncFlowAccessApi
import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.impl.AsyncFlowAccessApiImpl
import com.rarible.blockchain.scanner.BlockchainScanner
import com.rarible.blockchain.scanner.EnableBlockchainScannerComponents
import com.rarible.blockchain.scanner.block.BlockRepository
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
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.logging.AdvancedByteBufFormat

@Configuration
@EnableBlockchainScannerComponents
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
    fun ipfsClient(): WebClient {
        return buildWebClient("ipfsClient", apiProperties.ipfsInnerUrl)
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
            buildWebClient("chainMonstersGraphQl", CHAIN_MONSTERS_GRAPH_QL)
        )
    }

    @Bean
    fun matrixWorldClient(): WebClient {
        return buildWebClient(
            "matrixWorldClient",
            MATRIX_WORLD_BASE_URL
        )
    }

    @Bean(name = ["mugenClient"])
    fun mugenClient(): WebClient {
        return WebClient.create(MUGEN_ART_BASE_URL)
    }

    @EventListener(ApplicationReadyEvent::class)
    fun configureFlow() {
        Contracts.values().forEach {
            it.register(Flow.DEFAULT_ADDRESS_REGISTRY)
        }
        Flow.DEFAULT_ADDRESS_REGISTRY.apply {
            register("0xRARIBLETOKEN", FlowAddress("0xebf4ae01d1284af8"), FlowChainId.TESTNET)
            register("0xTOPSHOTROYALTIES", FlowAddress("0xebf4ae01d1284af8"), FlowChainId.TESTNET)
            register("0xMUGENNFT", FlowAddress("0xebf4ae01d1284af8"), FlowChainId.TESTNET)
            register("0xVERSUSART", FlowAddress("0x99ca04281098b33d"), FlowChainId.TESTNET)
            register("0xMETADATAVIEWS", FlowAddress("0x631e88ae7f1d7c20"), FlowChainId.TESTNET)

            register("0xRARIBLETOKEN", FlowAddress("0x01ab36aaf654a13e"), FlowChainId.MAINNET)
            register("0xTOPSHOTROYALTIES", FlowAddress("0xbd69b6abdfcf4539"), FlowChainId.MAINNET)
            register("0xMUGENNFT", FlowAddress("0x2cd46d41da4ce262"), FlowChainId.MAINNET)
            register("0xVERSUSART", FlowAddress("0xd796ff17107bbff6"), FlowChainId.MAINNET)
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

    companion object {

        const val MATRIX_WORLD_BASE_URL = "https://api.matrixworld.org/land/api/v1/land/metadata/estate/flow/"
        const val CHAIN_MONSTERS_GRAPH_QL = "https://europe-west3-chainmonstersmmo.cloudfunctions.net/graphql"
        const val MUGEN_ART_BASE_URL = "https://onchain.mugenart.io/flow/nft/0x2cd46d41da4ce262/metadata"
    }
}
