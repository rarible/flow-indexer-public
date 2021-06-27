package com.rarible.flow.core.consul

import com.rarible.core.application.ApplicationEnvironmentInfo
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.info.GitProperties
import org.springframework.cloud.consul.serviceregistry.ConsulRegistrationCustomizer
import org.springframework.context.annotation.Bean


@ConditionalOnProperty(prefix = "spring.cloud.service-registry.auto-registration", name = ["enabled"], havingValue = "true")
class ConsulAutoConfiguration(
    private val environmentInfo: ApplicationEnvironmentInfo,
    private val gitProperties: GitProperties?
) {
    val logger = LoggerFactory.getLogger(ConsulAutoConfiguration::class.java)

    @Bean
    fun customizer(): ConsulRegistrationCustomizer {
        logger.info("ConsulRegistrationCustomizer init...")
        return ConsulRegistrationCustomizer { consulRegistration ->
            consulRegistration.service.apply {
                val defaultTags = listOfNotNull(
                    "platform=spring-boot",
                    "env=${environmentInfo.name}",
                    gitProperties?.let { "commitHash=${it.commitId}" }
                )
                name = name?.let { "${environmentInfo.name}-$it" }
                logger.info("Consul service registration name - [$name]")
                tags = (tags ?: emptyList()) + defaultTags
                id = "$id:${environmentInfo.host}"
            }
        }
    }
}