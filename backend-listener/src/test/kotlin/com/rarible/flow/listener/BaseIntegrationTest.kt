package com.rarible.flow.listener

import com.rarible.core.test.ext.KafkaTest
import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
import com.rarible.flow.listener.config.ListenerProperties
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import javax.annotation.PostConstruct

@MongoTest
@MongoCleanup
@KafkaTest
abstract class BaseIntegrationTest {

    @Autowired
    protected lateinit var mongo: ReactiveMongoOperations

    @Autowired
    protected lateinit var listenerProperties: ListenerProperties

    @Autowired
    protected lateinit var protocolEventPublisher: ProtocolEventPublisher

    @LocalServerPort
    private var port: Int = 0

    @PostConstruct
    fun setup() {

    }
}
