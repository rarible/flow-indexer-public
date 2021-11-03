package com.rarible.flow.migrations.event

import com.rarible.flow.log.Log
import io.mongock.runner.spring.base.events.SpringMigrationFailureEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import kotlin.system.exitProcess


@Component
class FailureListener : ApplicationListener<SpringMigrationFailureEvent> {

    override fun onApplicationEvent(event: SpringMigrationFailureEvent) {
        log.error("Migration was finished with failure", event.migrationResult.exception)
        exitProcess(-1)
    }

    companion object {
        val log by Log()
    }
}