package com.rarible.flow.api.config

import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowAccessApi
import com.rarible.flow.api.service.FlowSignatureService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.converter.MessageConverter
import org.springframework.messaging.converter.StringMessageConverter
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class Config(
    val apiProperties: ApiProperties
): WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic")
        registry.setApplicationDestinationPrefixes("/api")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws").
            setAllowedOrigins("*") //TODO configure
            .withSockJS()
    }

    override fun configureMessageConverters(messageConverters: MutableList<MessageConverter>): Boolean {
        messageConverters.add(StringMessageConverter())
        messageConverters.add(MappingJackson2MessageConverter())
        return true
    }

    @Bean
    fun signatureService(
        resourceLoader: ResourceLoader
    ): FlowSignatureService {
        val sigVerify = resourceLoader.getResource("classpath:script/sig_verify.cdc").file.readText()
        return FlowSignatureService(
            sigVerify,
            apiProperties.chainId,
            Flow.newAccessApi(apiProperties.flowAccessUrl, apiProperties.flowAccessPort)
        )
    }
}
