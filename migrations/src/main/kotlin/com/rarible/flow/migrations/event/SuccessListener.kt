package com.rarible.flow.migrations.event

import com.rarible.flow.log.Log
import io.mongock.runner.spring.base.events.SpringMigrationSuccessEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import kotlin.system.exitProcess

@Component
class SuccessListener : ApplicationListener<SpringMigrationSuccessEvent> {

    override fun onApplicationEvent(event: SpringMigrationSuccessEvent) {
        log.info("Migration was finished successfully : {}", event.timestamp)
        exitProcess(0)
    }

    companion object {
        val log by Log()
    }
}