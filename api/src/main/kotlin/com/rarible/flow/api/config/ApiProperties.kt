package com.rarible.flow.api.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("flow-indexer-api")
data class ApiProperties(
    val ipfs: IpfsProperties,
    val httpClient: HttpClientProperties = HttpClientProperties(),
)

data class IpfsProperties(
    val internalGateway: String,
    val publicGateway: String
)

data class HttpClientProperties(
    val proxy: ProxyProperties = ProxyProperties(),
    val requestTimeout: Long = 60000
)

data class ProxyProperties(
    val url: String = "",
    val readTimeout: Int = 30000,
    val connectTimeout: Int = 30000,
    val requestTimeout: Long = 60000
)
