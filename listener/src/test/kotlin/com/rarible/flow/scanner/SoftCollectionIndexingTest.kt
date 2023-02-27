package com.rarible.flow.scanner

import com.nftco.flow.sdk.AddressRegistry.Companion.NON_FUNGIBLE_TOKEN
import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowAccessApi
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.FlowId
import com.nftco.flow.sdk.FlowTransactionStatus
import com.nftco.flow.sdk.cadence.JsonCadenceBuilder
import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.nftco.flow.sdk.cadence.UInt64NumberField
import com.nftco.flow.sdk.crypto.Crypto
import com.nftco.flow.sdk.simpleFlowTransaction
import com.rarible.blockchain.scanner.flow.model.FlowLog
import com.rarible.blockchain.scanner.flow.service.Spork
import com.rarible.blockchain.scanner.flow.service.SporkService
import com.rarible.core.test.containers.KGenericContainer
import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemCollection
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.event.EventId
import com.rarible.flow.core.event.RaribleNFTv2Meta
import com.rarible.flow.core.util.Log
import com.rarible.flow.scanner.emulator.EmulatorUser
import java.lang.Thread.sleep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.core.io.Resource
import org.springframework.data.mapping.div
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.utility.MountableFile

@IntegrationTest
@MongoTest
@MongoCleanup
@Disabled("Enable after Q2 2022")
class SoftCollectionIndexingTest {

    private lateinit var accessApi: FlowAccessApi

    @Value("classpath:emulator/transactions/soft-collection/create-minter.cdc")
    private lateinit var collectionMintTx: Resource

    @Value("classpath:emulator/transactions/soft-collection/setup_account.cdc")
    private lateinit var setupAccountTx: Resource

    @Value("classpath:emulator/transactions/soft-collection/update-minter.cdc")
    private lateinit var collectionUpdateTx: Resource

    @Value("classpath:emulator/transactions/soft-collection/create-item.cdc")
    private lateinit var createItemTx: Resource

    @Value("classpath:emulator/transactions/soft-collection/setup_item_account.cdc")
    private lateinit var setupItemAccTx: Resource

    @Value("classpath:emulator/transactions/soft-collection/transfer-item.cdc")
    private lateinit var transferItemTx: Resource

    @Value("classpath:emulator/transactions/soft-collection/burn-item.cdc")
    private lateinit var burnItemTx: Resource

    @Autowired
    private lateinit var mongo: ReactiveMongoTemplate

    @TestConfiguration
    class Conf {

        @Bean
        fun appListener(sporkService: SporkService): ApplicationListener<ApplicationReadyEvent> {
            return ApplicationListener<ApplicationReadyEvent> {
                sporkService.replace(FlowChainId.EMULATOR, listOf(
                    Spork(
                        from = 1L,
                        nodeUrl = flowEmulator.host,
                        port = flowEmulator.getMappedPort(3569)
                    ))
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
                register("0xRARIBLENFTV2", EmulatorUser.Emulator.address, FlowChainId.EMULATOR)
                register("0xSOFTCOLLECTION", EmulatorUser.Emulator.address, FlowChainId.EMULATOR)
            }

        }
    }

    @BeforeEach
    internal fun setUp() {
        accessApi = Flow.newAccessApi(host = flowEmulator.host, port = flowEmulator.getMappedPort(3569))
    }

    private val cb = JsonCadenceBuilder()

    @Test
    internal fun mintAndUpdateCollectionIndexingTest() = runBlocking {
        val payerKey = accessApi.getAccountAtLatestBlock(EmulatorUser.Patrick.address)!!.keys[0]
        val signer = Crypto.getSigner(
            privateKey = Crypto.decodePrivateKey(EmulatorUser.Patrick.keyHex),
            hashAlgo = payerKey.hashAlgo
        )


        val setup = accessApi.simpleFlowTransaction(address = EmulatorUser.Patrick.address, signer = signer) {
            script(setupAccountTx.inputStream.bufferedReader().use { it.readText() }, FlowChainId.EMULATOR)
        }.sendAndWaitForSeal()
        Assertions.assertTrue(setup.errorMessage.isEmpty(), "Setup account failed: ${setup.errorMessage}")
        val expectedCollection = ItemCollection(
            id = "A.${EmulatorUser.Emulator.address.base16Value}.SoftCollection.0",
            name = "Awesome Collection",
            symbol = "AC",
            isSoft = true,
            owner = EmulatorUser.Patrick.address,
            features = setOf("BURN", "SECONDARY_SALE_FEES"),
            chainId = 0L,
            description = "Description",
            icon = "Some icon",
            url = "https://google.com/"
        )

        val tx = accessApi.simpleFlowTransaction(address = EmulatorUser.Patrick.address, signer = signer) {
            script(collectionMintTx.inputStream.bufferedReader().use { it.readText() }, FlowChainId.EMULATOR)
            argument(cb.address(EmulatorUser.Patrick.address.formatted))
            argument(cb.optional(null))
            argument(cb.string("Awesome Collection"))
            argument(cb.string("AC"))
            argument(cb.optional(cb.string("Some icon")))
            argument(cb.optional(cb.string("Description")))
            argument(cb.optional(cb.string("https://google.com/")))
            argument(cb.optional(null))
            argument(cb.dictionary { emptyArray() })
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
        Assertions.assertEquals(expectedCollection.description,
            collection.description,
            "Collection description is incorrect")
        Assertions.assertEquals(expectedCollection.symbol, collection.symbol, "Collection symbol is incorrect")
        Assertions.assertEquals(expectedCollection.chainId, collection.chainId, "Collection chainId is incorrect ")
        Assertions.assertEquals(expectedCollection.icon, collection.icon, "Collection icon is incorrect ")
        Assertions.assertEquals(expectedCollection.url, collection.url, "Collection url is incorrect ")

        val updated = expectedCollection.copy(
            description = "New Description",
            icon = "New Icon",
        )

        val updateTx = accessApi.simpleFlowTransaction(address = EmulatorUser.Patrick.address, signer = signer) {
            script(collectionUpdateTx.inputStream.bufferedReader().use { it.readText() }, FlowChainId.EMULATOR)
            argument { cb.uint64(updated.chainId!!) }
            argument { cb.optional(cb.string(updated.url!!)) }
            argument { cb.optional(cb.string(updated.description!!)) }
            argument { cb.optional(cb.string(updated.icon!!)) }
        }.sendAndWaitForSeal()

        Assertions.assertTrue(updateTx.errorMessage.isEmpty(), "Update collection failed: ${updateTx.errorMessage}")
        withTimeout(30_000L) {
            var founded = false
            while (!founded) {
                val query = Query.query(
                    where(FlowLogEvent::log / FlowLog::transactionHash).isEqualTo(updateTx.events.first().transactionId.base16Value)
                )
                founded = mongo.exists(query, FlowLogEvent::class.java).awaitSingle()
            }
            withContext(Dispatchers.IO) {
                sleep(1000L)
            }
        }
        val c = mongo.findById(updated.id, ItemCollection::class.java).awaitSingle()
        Assertions.assertNotNull(c)
        Assertions.assertEquals(updated.id, c.id, "Updated collection id is wrong")
        Assertions.assertEquals(updated.description, c.description, "Updated collection description is wrong")
        Assertions.assertEquals(updated.url, c.url, "Updated collection url is wrong")
        Assertions.assertEquals(updated.icon, c.icon, "Updated collection icon is wrong")
        val mintTx = accessApi.simpleFlowTransaction(EmulatorUser.Patrick.address, signer) {
            script(createItemTx.inputStream.bufferedReader().use { it.readText() }, FlowChainId.EMULATOR)
            argument { cb.uint64(updated.chainId!!) }
            argument {
                cb.marshall(RaribleNFTv2Meta(
                    name = "First Awesome Item",
                    description = "Item description",
                    cid = "QmNe7Hd9xiqm1MXPtQQjVtksvWX6ieq9Wr6kgtqFo9D4CU",
                    attributes = emptyMap(),
                    contentUrls = emptyList()
                ), RaribleNFTv2Meta::class, EmulatorUser.Emulator.address)
            }
            argument { cb.array { emptyList() } }
        }.sendAndGetResult()
        Assertions.assertNotNull(mintTx.first)
        Assertions.assertTrue(mintTx.second.errorMessage.isEmpty(), "Mint item failed ${mintTx.second.errorMessage}")
        withTimeout(30_000L) { awaitLogEventByTxId(mintTx.first) }

        val itemId = ItemId(
            contract = EventId.of(mintTx.second.events.first().id).collection(),
            tokenId = JsonCadenceParser().long(mintTx.second.events.first().getField<UInt64NumberField>("id")!!)
        )
        var item = mongo.find(Query.query(
            where(Item::id).isEqualTo(itemId)
        ), Item::class.java).awaitSingle()
        Assertions.assertNotNull(item)
        Assertions.assertEquals(EmulatorUser.Emulator.address, item.creator, "Item creator wrong!")
        Assertions.assertEquals(EmulatorUser.Patrick.address, item.owner, "Item owner wrong!")

        val sqKey = accessApi.getAccountAtLatestBlock(EmulatorUser.Squidward.address)!!.keys[0]
        val sqSigner = Crypto.getSigner(
            privateKey = Crypto.decodePrivateKey(EmulatorUser.Squidward.keyHex),
            hashAlgo = sqKey.hashAlgo
        )
        accessApi.simpleFlowTransaction(EmulatorUser.Squidward.address, sqSigner) {
            script(setupItemAccTx.inputStream.bufferedReader().use { it.readText() }, FlowChainId.EMULATOR)
        }.sendAndWaitForSeal()

        val transferTx = accessApi.simpleFlowTransaction(EmulatorUser.Patrick.address, signer) {
            script(transferItemTx.inputStream.bufferedReader().use { it.readText() }, FlowChainId.EMULATOR)
            argument { cb.uint64(itemId.tokenId) }
            argument { cb.address(EmulatorUser.Squidward.address) }
        }.sendAndGetResult()

        Assertions.assertTrue(transferTx.second.errorMessage.isEmpty(),
            "Transfer failed! ${transferTx.second.errorMessage}")
        withTimeout(30_000L) { awaitLogEventByTxId(transferTx.first) }

        item = mongo.find(Query.query(
            where(Item::id).isEqualTo(itemId)
        ), Item::class.java).awaitSingle()

        Assertions.assertNotNull(item)
        Assertions.assertEquals(EmulatorUser.Emulator.address, item.creator, "Item creator wrong!")
        Assertions.assertEquals(EmulatorUser.Squidward.address, item.owner, "Item owner wrong!")


        val burnTx = accessApi.simpleFlowTransaction(EmulatorUser.Squidward.address, sqSigner) {
            script(burnItemTx.inputStream.bufferedReader().use { it.readText() }, FlowChainId.EMULATOR)
            argument { cb.uint64(itemId.tokenId) }
        }.sendAndGetResult()

        Assertions.assertTrue(burnTx.second.errorMessage.isEmpty(), "Burn failed! ${transferTx.second.errorMessage}")
        withTimeout(30_000L) { awaitLogEventByTxId(burnTx.first) }

        item = mongo.find(Query.query(
            where(Item::id).isEqualTo(itemId)
        ), Item::class.java).awaitSingle()

        Assertions.assertNotNull(item)
        Assertions.assertEquals(EmulatorUser.Emulator.address, item.creator, "Item creator wrong!")
        Assertions.assertNull(item.owner, "Owner must be null after BURN! [${item.owner}]")
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

    private suspend fun awaitLogEventByTxId(txId: FlowId) {
        var founded = false
        while (!founded) {
            val query = Query.query(
                where(FlowLogEvent::log / FlowLog::transactionHash).isEqualTo(txId.base16Value)
            )
            founded = mongo.exists(query, FlowLogEvent::class.java).awaitSingle()
            withContext(Dispatchers.IO) {
                sleep(1000L)
            }
        }
    }
}
