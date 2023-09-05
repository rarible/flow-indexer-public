package com.rarible.flow.core.config

import com.nftco.flow.sdk.FlowChainId
import com.rarible.core.kafka.Compression
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Profile

@ConstructorBinding
@ConfigurationProperties(prefix = "app")
@Profile("!without-kafka")
data class AppProperties(
    val environment: String,
    val flowAccessUrl: String,
    val flowAccessPort: Int,
    val kafkaReplicaSet: String,
    val compression: Compression = Compression.SNAPPY,
    val chainId: FlowChainId,
    val webApiUrl: String,
    val featureFlags: FeatureFlagsProperties = FeatureFlagsProperties(),
    val metricRootPath: String = "protocol.flow.indexer"
)

data class FeatureFlagsProperties(
    val enableRaribleCard: Boolean = true,
    val enableRariblePack: Boolean = true,
    val enableRariblePackV2: Boolean = true,
    val enableRaribleNft: Boolean = true,
    val enableStorefrontV1: Boolean = true,
    val enableStorefrontV2: Boolean = true,
    val enableProxyForMetaDownload: Boolean = false,
    val enableRawOnChainMetaCacheRead: Boolean = true,
    val enableRawOnChainMetaCacheWrite: Boolean = true,
    val enableBarbieDefaultVideoContentType: Boolean = true
)
