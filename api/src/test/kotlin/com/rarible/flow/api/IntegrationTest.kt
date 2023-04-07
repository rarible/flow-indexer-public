package com.rarible.flow.api

import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers

@Retention
@AutoConfigureJson
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "application.environment = test",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "spring.cloud.consul.config.enabled = false",
        "logging.logstash.tcp-socket.enabled = false",
        "logging.logjson.enabled = false",
    ]
)
@MongoTest
@MongoCleanup
@ActiveProfiles("test")
@Testcontainers
@Import(TestPropertiesConfiguration::class)
annotation class IntegrationTest
