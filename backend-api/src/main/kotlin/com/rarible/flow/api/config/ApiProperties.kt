package com.rarible.flow.api.config

import com.nftco.flow.sdk.FlowChainId
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("flow-indexer-api")
data class ApiProperties(
    val flowAccessUrl: String,
    val flowAccessPort: Int,
    val chainId: FlowChainId,
    val alchemyApiKey: String
)
