package com.rarible.flow.core.repository

import com.nftco.flow.sdk.FlowAddress
import com.rarible.core.test.ext.MongoTest
import com.rarible.flow.core.TestBeanConfiguration
import com.rarible.flow.core.config.CoreConfig
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.repository.data.randomAddress
import com.rarible.flow.core.repository.data.randomLong
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import reactor.test.StepVerifier
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit

@MongoTest
@DataMongoTest(
    properties = [
        "application.environment = dev",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "spring.cloud.consul.config.enabled = false",
        "logging.logstash.tcp-socket.enabled = false",
        "spring.data.mongodb.auto-index-creation = true"
    ]
)
@ContextConfiguration(classes = [CoreConfig::class, TestBeanConfiguration::class])
@ActiveProfiles("test")
class ItemRepositoryTest {

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @BeforeEach
    internal fun setUp() {
        itemRepository.deleteAll().block()
    }

    @Test
    internal fun `should save item and read item`() {
        val now = Instant.now(Clock.systemUTC())
        val tokenId = randomLong()

        val items = mutableMapOf<Long, Item>()
        for (i in tokenId..tokenId + 2) {
            items[i] =
                Item(
                    contract = "A.${randomAddress()}.RaribleNFT.NFT",
                    tokenId = i,
                    creator = FlowAddress(randomAddress()),
                    royalties = listOf(),
                    owner = FlowAddress(randomAddress()),
                    mintedAt = now.plus(i, ChronoUnit.MILLIS),
                    meta = randomAddress(),
                    collection = randomAddress(),
                    updatedAt = Instant.now()
                )
        }

        StepVerifier.create(itemRepository.saveAll(items.values.toList()))
            .assertNext { savedItem ->
                Assertions.assertNotNull(savedItem)
                println("process: ${savedItem.tokenId}")
                val item = items[savedItem.tokenId]
                Assertions.assertNotNull(item)
                item as Item
                Assertions.assertEquals(item.id, savedItem.id)
                Assertions.assertEquals(item.contract, savedItem.contract)
                Assertions.assertEquals(item.tokenId, savedItem.tokenId)
                Assertions.assertEquals(item.creator, savedItem.creator)
                Assertions.assertEquals(item.owner, savedItem.owner)
                Assertions.assertEquals(item.mintedAt, savedItem.mintedAt)
                Assertions.assertEquals(item.meta, savedItem.meta)
                Assertions.assertEquals(item.collection, savedItem.collection)
            }
            .assertNext { savedItem ->
                Assertions.assertNotNull(savedItem)
                println("process: ${savedItem.tokenId}")
                val item = items[savedItem.tokenId]
                Assertions.assertNotNull(item)
                item as Item
                Assertions.assertEquals(item.id, savedItem.id)
                Assertions.assertEquals(item.contract, savedItem.contract)
                Assertions.assertEquals(item.tokenId, savedItem.tokenId)
                Assertions.assertEquals(item.creator, savedItem.creator)
                Assertions.assertEquals(item.owner, savedItem.owner)
                Assertions.assertEquals(item.mintedAt, savedItem.mintedAt)
                Assertions.assertEquals(item.meta, savedItem.meta)
                Assertions.assertEquals(item.collection, savedItem.collection)
            }
            .assertNext { savedItem ->
                Assertions.assertNotNull(savedItem)
                println("process: ${savedItem.tokenId}")
                val item = items[savedItem.tokenId]
                Assertions.assertNotNull(item)
                item as Item
                Assertions.assertEquals(item.id, savedItem.id)
                Assertions.assertEquals(item.contract, savedItem.contract)
                Assertions.assertEquals(item.tokenId, savedItem.tokenId)
                Assertions.assertEquals(item.creator, savedItem.creator)
                Assertions.assertEquals(item.owner, savedItem.owner)
                Assertions.assertEquals(item.mintedAt, savedItem.mintedAt)
                Assertions.assertEquals(item.meta, savedItem.meta)
                Assertions.assertEquals(item.collection, savedItem.collection)
            }
            .verifyComplete()
    }
}
