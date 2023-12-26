package com.rarible.flow.core.test

import com.rarible.core.test.ext.MongoTest
import com.rarible.flow.core.config.CoreConfig
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@MongoTest
@SpringBootTest(
    properties = [
        "application.environment = dev",
        "application.serviceName = core",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "spring.cloud.consul.config.enabled = false",
        "logging.logstash.tcp-socket.enabled = false"
    ]
)
@ContextConfiguration(classes = [CoreConfig::class, TestConfiguration::class])
@ActiveProfiles("core", "test")
annotation class IntegrationTest
