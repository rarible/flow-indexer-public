package com.rarible.flow.core.config

import com.nftco.flow.sdk.FlowChainId
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val environment: String,
    val kafkaReplicaSet: String,
    val chainId: FlowChainId
)
