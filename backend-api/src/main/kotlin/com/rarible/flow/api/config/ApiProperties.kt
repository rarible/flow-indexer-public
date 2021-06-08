package com.rarible.flow.api.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("flow-indexer-api")
data class ApiProperties(
    val kafkaReplicaSet: String,
    val environment: String
)