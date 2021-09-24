package com.rarible.flow.api.config

import com.nftco.flow.sdk.Flow
import com.rarible.flow.api.service.FlowSignatureService
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

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
}
