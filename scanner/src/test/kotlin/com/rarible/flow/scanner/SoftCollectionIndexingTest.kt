package com.rarible.flow.scanner

import com.nftco.flow.sdk.*
import com.nftco.flow.sdk.AddressRegistry.Companion.NON_FUNGIBLE_TOKEN
import com.nftco.flow.sdk.cadence.JsonCadenceBuilder
import com.nftco.flow.sdk.crypto.Crypto
import com.rarible.blockchain.scanner.flow.service.SporkService
import com.rarible.core.test.containers.KGenericContainer
import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
import com.rarible.flow.core.domain.ItemCollection
import com.rarible.flow.log.Log
import com.rarible.flow.scanner.emulator.EmulatorUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.core.io.Resource
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.utility.MountableFile
import java.lang.Thread.sleep

@IntegrationTest
@MongoTest
@MongoCleanup
class SoftCollectionIndexingTest {

    private lateinit var accessApi: FlowAccessApi

    @Value("classpath:emulator/transactions/soft-collection/create-minter.cdc")
    private lateinit var collectionMintTx: Resource

    @Value("classpath:emulator/transactions/soft-collection/setup_account.cdc")
    private lateinit var setupAccountTx: Resource

    @Autowired
    private lateinit var mongo: ReactiveMongoTemplate

    @TestConfiguration
    class Conf {

        @Bean
        fun appListener(sporkService: SporkService): ApplicationListener<ApplicationReadyEvent> {
            return ApplicationListener<ApplicationReadyEvent> {
                sporkService.allSporks[FlowChainId.EMULATOR] = listOf(
                    SporkService.Spork(
                        from = 1L,
                        nodeUrl = flowEmulator.host,
                        port = flowEmulator.getMappedPort(3569)
                    )
                )
            }
        }
    }

    companion object {
        private val logger by Log()

        @Container
        val flowEmulator: KGenericContainer = KGenericContainer(
            "zolt85/flow-cli-emulator:latest"
        ).withEnv("FLOW_BLOCKTIME", "1s").withEnv("FLOW_WITHCONTRACTS", "true")
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("emulator/contracts"),
                "/root/contracts"
            )
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("emulator/flow.json"),
                "/root/flow.json"
            )
            .withExposedPorts(3569, 8080)
            .withLogConsumer {
                logger.info("EMU: ${it.utf8String}")
            }
            .withReuse(true)
            .withCommand("flow emulator")
            .waitingFor(Wait.forHttp("/").forPort(8080).forStatusCode(500))


        @BeforeAll
        @JvmStatic
        internal fun setup() {
            logger.info("===== SETUP =====")
            logger.info(flowEmulator.execInContainer("flow", "project", "deploy").stdout)
            logger.info(flowEmulator.execInContainer("flow",
                "accounts",
                "create",
                "--key",
                EmulatorUser.Patrick.pubHex).stdout)
            logger.info(flowEmulator.execInContainer("flow",
                "accounts",
                "create",
                "--key",
                EmulatorUser.Squidward.pubHex).stdout)
            logger.info(flowEmulator.execInContainer("flow",
                "accounts",
                "create",
                "--key",
                EmulatorUser.Gary.pubHex).stdout)
            with(Flow.DEFAULT_ADDRESS_REGISTRY) {
                register(NON_FUNGIBLE_TOKEN, EmulatorUser.Emulator.address, FlowChainId.EMULATOR)
                register("0xRARIBLENFT_V2", EmulatorUser.Emulator.address, FlowChainId.EMULATOR)
                register("0xSOFTCOLLECTION", EmulatorUser.Emulator.address, FlowChainId.EMULATOR)

            }

        }
    }

    @BeforeEach
    internal fun setUp() {
        accessApi = Flow.newAccessApi(host = flowEmulator.host, port = flowEmulator.getMappedPort(3569))
    }

    @Test
    internal fun mintCollectionIndexingTest() = runBlocking {
        // todo change to not Emulator account after minter changes
        val payerKey = accessApi.getAccountAtLatestBlock(EmulatorUser.Emulator.address)!!.keys[0]
        val signer = Crypto.getSigner(
            privateKey = Crypto.decodePrivateKey(EmulatorUser.Emulator.keyHex),
            hashAlgo = payerKey.hashAlgo
        )
        val cb = JsonCadenceBuilder()

        val setup = accessApi.simpleFlowTransaction(address = EmulatorUser.Emulator.address, signer = signer) {
            script(setupAccountTx.inputStream.bufferedReader().use { it.readText() }, FlowChainId.EMULATOR)
        }.sendAndWaitForSeal()
        Assertions.assertTrue(setup.errorMessage.isEmpty(), "Setup account failed: ${setup.errorMessage}")
        val expectedCollection = ItemCollection(
            id = "A.${EmulatorUser.Emulator.address.base16Value}.Awesome_Collection",
            name = "Awesome Collection",
            symbol = "AC",
            isSoft = true,
            owner = EmulatorUser.Emulator.address,
            features = setOf("BURN", "SECONDARY_SALE_FEES"),
            chainId = 0L,
            description = "Description",
            icon = "Some icon",
            url = "https://google.com/"
        )

        val tx = accessApi.simpleFlowTransaction(address = EmulatorUser.Emulator.address, signer = signer) {
            script(collectionMintTx.inputStream.bufferedReader().use { it.readText() }, FlowChainId.EMULATOR)
            argument(cb.address(EmulatorUser.Emulator.address.formatted))
            argument(cb.optional(null))
            argument(cb.string("Awesome Collection"))
            argument(cb.string("AC"))
            argument(cb.optional(cb.string("Some icon")))
            argument(cb.optional(cb.string("Description")))
            argument(cb.optional(cb.string("https://google.com/")))
            argument(cb.optional(null))
            argument(cb.array { emptyList() })
        }.sendAndWaitForSeal()
        logger.info("tx result: $tx")
        Assertions.assertNotNull(tx, "Tx result is null!")
        Assertions.assertEquals(FlowTransactionStatus.SEALED, tx.status, "Status not SEALED")
        Assertions.assertTrue(tx.errorMessage.isEmpty(), "Error message is not empty! [${tx.errorMessage}]")
        Assertions.assertTrue(tx.events.isNotEmpty(), "Tx have no events!")
        val collection = withTimeout(60_000L) {
            waitFromDb(expectedCollection.id)
        }
        Assertions.assertEquals(expectedCollection.id, collection.id, "Collection id is incorrect")
        Assertions.assertEquals(expectedCollection.name, collection.name, "Collection name is incorrect")
        Assertions.assertTrue(collection.isSoft, "Collection is not marked as Soft")
        Assertions.assertEquals(expectedCollection.description, collection.description, "Collection description is incorrect")
        Assertions.assertEquals(expectedCollection.symbol, collection.symbol, "Collection symbol is incorrect")
        Assertions.assertEquals(expectedCollection.chainId, collection.chainId, "Collection chainId is incorrect ")
        Assertions.assertEquals(expectedCollection.icon, collection.icon, "Collection icon is incorrect ")
        Assertions.assertEquals(expectedCollection.url, collection.url, "Collection url is incorrect ")
    }

    private suspend fun waitFromDb(id: String): ItemCollection {
        var c = mongo.findById(id, ItemCollection::class.java).awaitSingleOrNull()
        while (c == null) {
            c = mongo.findById(id, ItemCollection::class.java).awaitSingleOrNull()
            withContext(Dispatchers.IO) {
                sleep(1000L)
            }
        }
        return c
    }
}
