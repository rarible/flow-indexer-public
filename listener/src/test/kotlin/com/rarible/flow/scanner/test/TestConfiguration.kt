package com.rarible.flow.scanner.test

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.service.Spork
import com.rarible.blockchain.scanner.flow.service.SporkService
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumerFactory
import com.rarible.core.kafka.RaribleKafkaConsumerSettings
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import com.rarible.core.test.ext.KafkaTestExtension
import com.rarible.protocol.dto.FlowNftItemEventDto
import com.rarible.protocol.dto.FlowNftItemEventTopicProvider
import com.rarible.protocol.dto.FlowNftOwnershipEventTopicProvider
import com.rarible.protocol.dto.FlowOwnershipEventDto
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean

@TestConfiguration
class TestConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {

    private val env = applicationEnvironmentInfo.name

    private val kafkaConsumerFactory = RaribleKafkaConsumerFactory(
        applicationEnvironmentInfo.name,
        applicationEnvironmentInfo.host
    )

    @Bean
    fun appListener(sporkService: SporkService): ApplicationListener<ApplicationReadyEvent> {
        return ApplicationListener<ApplicationReadyEvent> {
            sporkService.replace(
                FlowChainId.EMULATOR, listOf(
                    Spork(
                        from = 0L,
                        to = Long.MAX_VALUE,
                        nodeUrl = FlowTestContainer.host(),
                        port = FlowTestContainer.port()
                    )
                )
            )
        }
    }

    @Bean
    fun testItemHandler() = TestFlowEventHandler<FlowNftItemEventDto>()

    @Bean
    fun testItemConsumer(
        handler: TestFlowEventHandler<FlowNftItemEventDto>
    ): RaribleKafkaConsumerWorker<FlowNftItemEventDto> {
        val topic = FlowNftItemEventTopicProvider.getTopic(env)
        val settings = RaribleKafkaConsumerSettings(
            hosts = KafkaTestExtension.kafkaContainer.kafkaBoostrapServers(),
            topic = topic,
            group = "test-union-item-group",
            concurrency = 1,
            batchSize = 10,
            async = false,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST,
            valueClass = FlowNftItemEventDto::class.java
        )
        return kafkaConsumerFactory.createWorker(settings, handler)
    }

    @Bean
    fun testOwnershipHandler() = TestFlowEventHandler<FlowOwnershipEventDto>()

    @Bean
    fun testOwnershipConsumer(
        handler: TestFlowEventHandler<FlowOwnershipEventDto>
    ): RaribleKafkaConsumerWorker<FlowOwnershipEventDto> {
        val topic = FlowNftOwnershipEventTopicProvider.getTopic(env)
        val settings = RaribleKafkaConsumerSettings(
            hosts = KafkaTestExtension.kafkaContainer.kafkaBoostrapServers(),
            topic = topic,
            group = "test-union-ownership-group",
            concurrency = 1,
            batchSize = 10,
            async = false,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST,
            valueClass = FlowOwnershipEventDto::class.java
        )
        return kafkaConsumerFactory.createWorker(settings, handler)
    }
}