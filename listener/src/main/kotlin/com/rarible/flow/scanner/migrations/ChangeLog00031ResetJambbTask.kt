package com.rarible.flow.scanner.migrations

import com.rarible.core.task.Task
import com.rarible.core.task.TaskStatus
import com.rarible.flow.Contracts
import com.rarible.flow.scanner.subscriber.flowDescriptorName
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo

//@ChangeUnit(
//    id = "ChangeLog00031ResetJambbTask",
//    order = "00031",
//    author = "flow"
//)
//class ChangeLog00031ResetJambbTask(
//    private val mongoTemplate: MongoTemplate,
//) {
//
//    @Execution
//    fun changeSet() {
//        mongoTemplate.updateFirst(
//            Query(
//                Task::param isEqualTo Contracts.JAMBB_MOMENTS.flowDescriptorName()
//            ),
//            Update.update(
//                Task::state.name, 20445936L
//            ).set(
//                Task::lastStatus.name, TaskStatus.NONE
//            ).set(
//                Task::running.name, false
//            ).set(
//                Task::version.name, 0
//            ).set(
//                Task::lastError.name, null
//            )
//            ,
//            Task::class.java
//        )
//    }
//
//    @RollbackExecution
//    fun rollBack() {
//    }
//}