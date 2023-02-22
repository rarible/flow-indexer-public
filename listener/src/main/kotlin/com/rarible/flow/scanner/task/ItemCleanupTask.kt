package com.rarible.flow.scanner.task

import com.rarible.core.task.TaskHandler
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.scanner.job.ItemCleanupJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component

@Component
class ItemCleanupTask(
    private val job: ItemCleanupJob
) : TaskHandler<String> {

    override val type = "ITEM_CLEANUP_TASK"

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val itemId = from?.let { ItemId.parse(it) }
        return job.execute(itemId).map { it.toString() }
    }
}

