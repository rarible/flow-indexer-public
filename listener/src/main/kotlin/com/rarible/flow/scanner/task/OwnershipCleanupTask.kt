package com.rarible.flow.scanner.task

import com.rarible.core.task.TaskHandler
import com.rarible.flow.core.domain.OwnershipId
import com.rarible.flow.scanner.job.OwnershipCleanupJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component

@Component
class OwnershipCleanupTask(
    private val job: OwnershipCleanupJob
) : TaskHandler<String> {

    override val type = "OWNERSHIP_CLEANUP_TASK"

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val ownershipId = from?.let { OwnershipId.parse(it) }
        return job.execute(ownershipId).map { it.toString() }
    }
}