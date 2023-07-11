package com.rarible.flow.scanner.test

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.service.Spork
import com.rarible.blockchain.scanner.flow.service.SporkService
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean

@TestConfiguration
class TestConfiguration {

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
}