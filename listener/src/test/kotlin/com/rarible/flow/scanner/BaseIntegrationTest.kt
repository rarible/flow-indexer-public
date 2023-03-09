package com.rarible.flow.scanner

import com.rarible.core.test.ext.KafkaTest
import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import javax.annotation.PostConstruct

@MongoTest
@MongoCleanup
@KafkaTest
abstract class BaseIntegrationTest : BaseJsonEventTest() {

    @Autowired
    protected lateinit var mongo: ReactiveMongoOperations

    @LocalServerPort
    private var port: Int = 0


    @PostConstruct
    fun setup() {

    }
}