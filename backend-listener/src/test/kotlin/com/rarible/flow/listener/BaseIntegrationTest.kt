package com.rarible.flow.listener

import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.test.ext.KafkaTest
import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
import com.rarible.flow.core.config.CoreConfig
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.listener.config.Config
import com.rarible.flow.listener.config.ListenerProperties
import com.rarible.flow.listener.handler.EventHandler
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import com.rarible.protocol.dto.FlowNftItemEventDto
import com.rarible.protocol.dto.FlowOwnershipEventDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import javax.annotation.PostConstruct

@MongoTest
@MongoCleanup
@KafkaTest
@Import(Config::class, CoreConfig::class)
abstract class BaseIntegrationTest {

    @Autowired
    protected lateinit var mongo: ReactiveMongoOperations

    @Autowired
    protected lateinit var listenerProperties: ListenerProperties

    @Autowired
    protected lateinit var protocolEventPublisher: ProtocolEventPublisher

    @Autowired
    lateinit var eventHandler: EventHandler

    @Autowired
    lateinit var itemRepository: ItemRepository

    @Autowired
    lateinit var ownershipRepository: OwnershipRepository

    @Autowired
    lateinit var itemHistoryRepository: ItemHistoryRepository

    @Autowired
    lateinit var orderRepository: OrderRepository

    @Autowired
    lateinit var itemEvents: RaribleKafkaConsumer<FlowNftItemEventDto>

    @Autowired
    lateinit var ownershipEvents: RaribleKafkaConsumer<FlowOwnershipEventDto>

    @LocalServerPort
    private var port: Int = 0

    @PostConstruct
    fun setup() {

    }
}
