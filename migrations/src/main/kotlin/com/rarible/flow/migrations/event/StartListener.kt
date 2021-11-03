package com.rarible.flow.migrations.event

import com.rarible.flow.log.Log
import io.mongock.runner.spring.base.events.SpringMigrationStartedEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import kotlin.system.exitProcess

@Component
class StartListener : ApplicationListener<SpringMigrationStartedEvent> {

    override fun onApplicationEvent(event: SpringMigrationStartedEvent) {
        log.info("Migration was started : {}", event.timestamp)
        exitProcess(0)
    }

    companion object {
        val log by Log()
    }
}