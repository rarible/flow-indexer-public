package com.rarible.flow.listener.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("flow-indexer-listener")
data class ListenerProperties(
    val kafkaReplicaSet: String,
    val environment: String
)