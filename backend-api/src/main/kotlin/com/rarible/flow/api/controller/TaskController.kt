package com.rarible.flow.api.controller

import com.rarible.flow.log.Log
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin
class TaskController(
    private val mongoTemplate: ReactiveMongoTemplate
) {

    @PostMapping("/v0.1/task/{taskId}")
    suspend fun updateTask(
        @PathVariable taskId: String,
        @RequestParam state: Long,
    ): ResponseEntity<String> {
        val result = mongoTemplate.updateFirst(
            Query(
                Criteria("_id")
                    .isEqualTo(ObjectId(taskId))
                    .and("running").isEqualTo(false)
            ),
            Update()
                .set("state", state)
                .set("version", 0L)
                .set("lastStatus", "NONE")
                .set("lastError", "")
                .set("running", false),
            "task"
        ).awaitFirstOrNull()

        return if (result == null || !result.wasAcknowledged()) {
            logger.error("Task {} was not updated due to error", taskId)
            ResponseEntity.notFound().build()
        } else {
            if (result.modifiedCount == 0L) {
               logger.warn("Task {} was not updated. Probably it is still running.", taskId)
            } else {
                logger.info("Task {} was updated", taskId)
            }
            ResponseEntity.ok().body("ok")
        }
    }

    companion object {
        private val logger by Log()
    }
}