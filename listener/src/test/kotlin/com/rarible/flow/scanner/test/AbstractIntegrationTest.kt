package com.rarible.flow.scanner.test

import com.rarible.protocol.dto.FlowNftItemDeleteEventDto
import com.rarible.protocol.dto.FlowNftItemEventDto
import com.rarible.protocol.dto.FlowNftItemUpdateEventDto
import com.rarible.protocol.dto.FlowNftOwnershipDeleteEventDto
import com.rarible.protocol.dto.FlowNftOwnershipUpdateEventDto
import com.rarible.protocol.dto.FlowOwnershipEventDto
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Query

@IntegrationTest
abstract class AbstractIntegrationTest : BaseJsonEventTest() {

    @Autowired
    protected lateinit var mongo: ReactiveMongoOperations

    @Autowired
    lateinit var testItemEventHandler: TestFlowEventHandler<FlowNftItemEventDto>

    @Autowired
    lateinit var testOwnershipEventHandler: TestFlowEventHandler<FlowOwnershipEventDto>

    @BeforeEach
    fun clearDatabase() {
        mongo.collectionNames
            .filter { !it.startsWith("system") && !it.equals("block") }
            .flatMap { mongo.remove(Query(), it) }
            .then().block()

        testItemEventHandler.events.clear()
        testOwnershipEventHandler.events.clear()
    }

    fun findItemUpdates(itemId: String): List<FlowNftItemUpdateEventDto> {
        return testItemEventHandler.events.filterIsInstance(FlowNftItemUpdateEventDto::class.java)
            .filter { it.itemId == itemId }
    }

    fun findItemDeletions(itemId: String): List<FlowNftItemDeleteEventDto> {
        return testItemEventHandler.events.filterIsInstance(FlowNftItemDeleteEventDto::class.java)
            .filter { it.itemId == itemId }
    }

    fun findOwnershipUpdates(ownershipId: String): List<FlowNftOwnershipUpdateEventDto> {
        return testOwnershipEventHandler.events.filterIsInstance(FlowNftOwnershipUpdateEventDto::class.java)
            .filter { it.ownershipId == ownershipId }
    }

    fun findOwnershipDeletions(ownershipId: String): List<FlowNftOwnershipDeleteEventDto> {
        return testOwnershipEventHandler.events.filterIsInstance(FlowNftOwnershipDeleteEventDto::class.java)
            .filter { it.ownershipId == ownershipId }
    }
}