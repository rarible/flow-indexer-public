package com.rarible.flow.core.repository

import com.rarible.core.test.ext.MongoTest
import com.rarible.flow.core.config.CoreConfig
import com.rarible.flow.core.domain.*
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.onflow.sdk.FlowAddress
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils
import java.math.BigDecimal
import java.util.*
import kotlin.random.Random

@MongoTest
@DataMongoTest(
    properties = [
        "application.environment = dev",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "spring.cloud.consul.config.enabled = false",
        "logging.logstash.tcp-socket.enabled = false"
    ]
)
@ContextConfiguration(classes = [CoreConfig::class])
@ActiveProfiles("test")
internal class OwnershipRepositoryTest(

) {
    @Autowired
    lateinit var ownershipRepository: OwnershipRepository

    @BeforeEach
    fun beforeEach() {
        ownershipRepository.deleteAll().block()
    }

    @Test
    fun `should delete and return ownerships`() {
        val contract = randomAddress()
        val owner = randomAddress()

        val (o1, o2, o3) = listOf(
            createOwnership(contract, owner),
            createOwnership(contract, owner),
            createOwnership(contract, owner)
        )
        ownershipRepository.saveAll(
            listOf(o1, o2, o3)
        )

        ownershipRepository.deleteAllByContractAndTokenId(o1.contract, o1.tokenId).subscribe {
            it shouldBeEqualToComparingFields o1
        }

    }


    private fun createOwnership(contract: FlowAddress = randomAddress(), owner: FlowAddress = randomAddress()) = Ownership(
        contract,
        Random.nextLong(),
        owner
    )

    fun randomAddress() = FlowAddress("0x${RandomStringUtils.random(16, "0123456789ABCDEF")}".lowercase(Locale.ENGLISH))
}