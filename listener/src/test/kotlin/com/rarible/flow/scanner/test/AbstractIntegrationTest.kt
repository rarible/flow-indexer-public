package com.rarible.flow.scanner.test

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Query

@IntegrationTest
abstract class AbstractIntegrationTest : BaseJsonEventTest() {

    @Autowired
    protected lateinit var mongo: ReactiveMongoOperations

    @BeforeEach
    fun clearDatabase() {
        mongo.collectionNames
            .filter { !it.startsWith("system") && !it.equals("block") }
            .flatMap { mongo.remove(Query(), it) }
            .then().block()
    }
}