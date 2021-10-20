package com.rarible.flow.scanner.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "scanner")
data class ScannerProperties(
    val kafkaReplicaSet: String,
    val environment: String,
    val defaultItemCollection: ItemCollectionProperty
)
