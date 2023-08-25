package com.rarible.flow.api.config

import com.netflix.graphql.dgs.client.MonoGraphQLClient
import com.netflix.graphql.dgs.client.WebClientGraphQLClient
import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.EnableFlowBlockchainScanner
import com.rarible.blockchain.scanner.flow.service.AsyncFlowAccessApi
import com.rarible.blockchain.scanner.flow.service.FlowApiFactory
import com.rarible.blockchain.scanner.flow.service.SporkService
import com.rarible.core.common.safeSplit
import com.rarible.core.meta.resource.http.DefaultHttpClient
import com.rarible.core.meta.resource.http.ExternalHttpClient
import com.rarible.core.meta.resource.http.ProxyHttpClient
import com.rarible.core.meta.resource.http.builder.DefaultWebClientBuilder
import com.rarible.core.meta.resource.http.builder.ProxyWebClientBuilder
import com.rarible.core.meta.resource.parser.UrlParser
import com.rarible.core.meta.resource.resolver.ConstantGatewayProvider
import com.rarible.core.meta.resource.resolver.IpfsGatewayResolver
import com.rarible.core.meta.resource.resolver.LegacyIpfsGatewaySubstitutor
import com.rarible.core.meta.resource.resolver.RandomGatewayProvider
import com.rarible.core.meta.resource.resolver.UrlResolver
import com.rarible.flow.Contracts
import com.rarible.flow.api.service.SignatureService
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
import org.springframework.http.HttpHeaders
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.logging.AdvancedByteBufFormat

@Configuration
@EnableFlowBlockchainScanner
@EnableConfigurationProperties(ApiProperties::class)
class Config(
    val appProperties: AppProperties,
    val apiProperties: ApiProperties,
    val sporkService: SporkService
) {

    @Suppress("PrivatePropertyName")
    private val DEFAULT_MESSAGE_SIZE: Int = 33554432 //32 Mb in bites

    @Bean
    fun ipfsProperties(): IpfsProperties {
        return apiProperties.ipfs
    }

    @Bean
    fun featureFlags(): FeatureFlags {
        return apiProperties.featureFlags
    }

    @Bean
    fun signatureService(api: AsyncFlowAccessApi): SignatureService {
        return SignatureService(
            appProperties.chainId,
            api
        )
    }

    @Bean
    fun api(flowApiFactory: FlowApiFactory): AsyncFlowAccessApi = flowApiFactory.getApi(sporkService.currentSpork())

    @Bean
    fun ipfsClient(): WebClient {
        return buildWebClient("ipfsClient", apiProperties.ipfs.internalGateway)
    }

    @Bean
    fun webClient(): WebClient {
        return WebClient.create()
    }

    @Bean
    fun urlParser() = UrlParser()

    @Bean
    fun urlResolver(ipfs: IpfsProperties): UrlResolver {
        val internalGatewayProvider = RandomGatewayProvider(
            ipfs.internalGateway.safeSplit().map { it.trimEnd('/') }
        )
        val publicGatewayProvider = ConstantGatewayProvider(
            ipfs.publicGateway.trimEnd('/')
        )

        return UrlResolver(
            ipfsGatewayResolver = IpfsGatewayResolver(
                publicGatewayProvider = publicGatewayProvider,
                internalGatewayProvider = internalGatewayProvider,
                customGatewaysResolver = LegacyIpfsGatewaySubstitutor(listOf())
            )
        )
    }

    @Bean
    fun externalHttpClient(): ExternalHttpClient {
        val httpClientProperties = apiProperties.httpClient
        val proxyProperties = httpClientProperties.proxy

        val followRedirect = true  // TODO Move to properties?

        val defaultHeaders = HttpHeaders()
        defaultHeaders.set(HttpHeaders.USER_AGENT, "rarible-protocol")

        val defaultWebClientBuilder = DefaultWebClientBuilder(
            followRedirect = followRedirect,
            defaultHeaders = defaultHeaders
        )
        val proxyWebClientBuilder = ProxyWebClientBuilder(
            readTimeout = proxyProperties.readTimeout,
            connectTimeout = proxyProperties.connectTimeout,
            proxyUrl = proxyProperties.url,
            followRedirect = followRedirect,
            defaultHeaders = defaultHeaders
        )

        val defaultHttpClient = DefaultHttpClient(
            builder = defaultWebClientBuilder,
            requestTimeout = httpClientProperties.requestTimeout

        )
        val proxyHttpClient = ProxyHttpClient(
            builder = proxyWebClientBuilder,
            requestTimeout = proxyProperties.requestTimeout
        )

        return ExternalHttpClient(
            defaultClient = defaultHttpClient,
            proxyClient = proxyHttpClient,
            customClients = listOf()
        )
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
