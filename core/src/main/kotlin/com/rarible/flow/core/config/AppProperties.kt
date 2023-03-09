package com.rarible.flow.core.config

import com.nftco.flow.sdk.FlowChainId
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Profile

@ConstructorBinding
@ConfigurationProperties(prefix = "app")
@Profile("!without-kafka")
data class AppProperties(
    val environment: String,
    val kafkaReplicaSet: String,
    val chainId: FlowChainId,
    val webApiUrl: String,
    val featureFlags: FeatureFlagsProperties = FeatureFlagsProperties()
)

data class FeatureFlagsProperties(
    val enableRaribleCard: Boolean = true,
    val enableRariblePack: Boolean = true,
    val enableRaribleNft: Boolean = true,
)
