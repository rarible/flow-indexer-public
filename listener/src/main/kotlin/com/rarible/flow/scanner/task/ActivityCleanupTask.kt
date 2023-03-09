package com.rarible.flow.scanner.task

import com.rarible.core.task.TaskHandler
import com.rarible.flow.scanner.config.FlowListenerProperties
import com.rarible.flow.scanner.job.ActivityCleanupJob
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Component

@Component
class ActivityCleanupTask(
    private val job: ActivityCleanupJob,
    private val properties: FlowListenerProperties
) : TaskHandler<String> {

    override val type = "ACTIVITY_CLEANUP_TASK"

    override suspend fun isAbleToRun(param: String): Boolean {
        return properties.cleanup.enabled
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        return job.execute(from)
    }
}