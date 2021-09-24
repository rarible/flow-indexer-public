package com.rarible.flow.api

import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
import com.rarible.flow.api.config.Config
import com.rarible.flow.core.config.CoreConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import javax.annotation.PostConstruct

@MongoTest
@MongoCleanup
abstract class BaseIntegrationTest {

    @Autowired
    protected lateinit var mongo: ReactiveMongoOperations

    @LocalServerPort
    private var port: Int = 0

    @PostConstruct
    fun setup() {

    }
}