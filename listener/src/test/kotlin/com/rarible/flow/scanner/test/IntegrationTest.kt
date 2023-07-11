package com.rarible.flow.scanner.test

import com.rarible.core.test.ext.KafkaTest
import com.rarible.core.test.ext.MongoTest
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers

@Retention
@AutoConfigureJson
@KafkaTest
@FlowTest
@MongoTest
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "application.environment = test",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "spring.cloud.config.enabled = false",
        "spring.cloud.consul.config.enabled = false",
        "logging.logstash.tcp-socket.enabled = false",
        "blockchain.scanner.flow.chainId = EMULATOR",
        "logging.logjson.enabled = false"
    ]
)
@ActiveProfiles("test")
@Import(TestConfiguration::class)
@Testcontainers
annotation class IntegrationTest
