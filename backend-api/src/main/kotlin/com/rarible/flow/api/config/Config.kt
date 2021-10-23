package com.rarible.flow.api.config

import com.nftco.flow.sdk.AsyncFlowAccessApi
import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.api.service.FlowSignatureService
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener

@Configuration
@EnableConfigurationProperties(ApiProperties::class)
class Config(
    val apiProperties: ApiProperties
) {

    @Bean
    fun signatureService(): FlowSignatureService {
        return FlowSignatureService(
            apiProperties.chainId,
            Flow.newAccessApi(apiProperties.flowAccessUrl, apiProperties.flowAccessPort)
        )
    }

    @Bean
    fun api(): AsyncFlowAccessApi = Flow.newAsyncAccessApi(apiProperties.flowAccessUrl, apiProperties.flowAccessPort)

    @EventListener(ApplicationReadyEvent::class)
    fun configureFlow() {
        Flow.DEFAULT_ADDRESS_REGISTRY.register("0xMOTOGPTOKEN", FlowAddress("0xa49cc0ee46c54bfb"), FlowChainId.MAINNET)
        Flow.DEFAULT_ADDRESS_REGISTRY.register("0xMOTOGPTOKEN", FlowAddress("0x01658d9b94068f3c"), FlowChainId.TESTNET)
        Flow.DEFAULT_ADDRESS_REGISTRY.register("0xEVOLUTIONTOKEN", FlowAddress("0xf4264ac8f3256818"), FlowChainId.MAINNET)
        Flow.DEFAULT_ADDRESS_REGISTRY.register("0xEVOLUTIONTOKEN", FlowAddress("0x01658d9b94068f3c"), FlowChainId.TESTNET)
        Flow.configureDefaults(chainId = apiProperties.chainId)
    }
}
