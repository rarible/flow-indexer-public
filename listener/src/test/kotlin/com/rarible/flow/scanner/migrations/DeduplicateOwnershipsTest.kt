package com.rarible.flow.scanner.migrations

//@MongoTest
//@DataMongoTest(
//    properties = [
//        "application.environment = dev",
//        "spring.cloud.service-registry.auto-registration.enabled = false",
//        "spring.cloud.discovery.enabled = false",
//        "spring.cloud.consul.config.enabled = false",
//        "logging.logstash.tcp-socket.enabled = false"
//    ]
//)
//@ContextConfiguration(classes = [CoreConfig::class])
//@ActiveProfiles("test")
//internal class DeduplicateOwnershipsTest {
//
//    private val logger by Log()
//
//    @Autowired
//    lateinit var itemRepository: ItemRepository
//
//    @Autowired
//    lateinit var mongoTemplate: ReactiveMongoTemplate
//
//
//    @Test
//    fun `should transform items`() {
//        val items = (1..100L).map {
//            Data.createItem().copy(tokenId = it, owner = FlowAddress(
//                String.format("0x%08x", it))
//            )
//        }
//
//        itemRepository.saveAll(items).blockLast()
//
//        val result = mongoTemplate.executeCommand(ChangeLog00009DeduplicateOwnerships.migrationCommand).block()
//
//        logger.info("Result: {}", result)
//
//        mongoTemplate.findAll<Ownership>("ownership_fix").collectList().block()!! shouldHaveSize 100
//    }
//}