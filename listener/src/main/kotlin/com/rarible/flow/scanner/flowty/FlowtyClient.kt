package com.rarible.flow.scanner.flowty

import com.rarible.flow.scanner.config.FlowListenerProperties
import io.netty.channel.ChannelOption
import io.netty.channel.epoll.EpollChannelOption
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import reactor.netty.transport.ProxyProvider
import java.net.URI

@Component
class FlowtyClient(
    properties: FlowListenerProperties,
) {
    private val transport = initTransport(
        URI(properties.flowty.endpoint),
        properties.flowty.proxy?.let { URI.create(it) }
    )

    suspend fun getGamisodesToken(tokenId: Long): GamisodesToken {
        return transport.get()
            .uri("nft/0x09e04bdbcccde6ca/Gamisodes/$tokenId")
            .retrieve()
            .bodyToMono<GamisodesToken>()
            .awaitFirst()
    }

    private fun initTransport(endpoint: URI, proxy: URI?): WebClient {
        return WebClient.builder().run {
            clientConnector(clientConnector(proxy))
            baseUrl(endpoint.toASCIIString())
            build()
        }
    }

    private fun clientConnector(proxy: URI?): ClientHttpConnector {
        val provider = ConnectionProvider.builder("flowty-connection-provider")
            .maxConnections(50)
            .pendingAcquireMaxCount(-1)
            .lifo()
            .build()

        val client = HttpClient.create(provider)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(EpollChannelOption.TCP_KEEPIDLE, 300)
            .option(EpollChannelOption.TCP_KEEPINTVL, 60)
            .option(EpollChannelOption.TCP_KEEPCNT, 8)

        val finalClient = if (proxy != null) {
            client
                .proxy { option ->
                    val userInfo = proxy.userInfo.split(":")
                    option
                        .type(ProxyProvider.Proxy.HTTP)
                        .host(proxy.host)
                        .username(userInfo[0])
                        .password { userInfo[1] }
                        .port(proxy.port)
                }
        } else {
            client
        }
        return ReactorClientHttpConnector(finalClient)
    }
}
