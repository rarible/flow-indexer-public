package com.rarible.flow.scanner

import com.nftco.flow.sdk.AddressRegistry.Companion.NON_FUNGIBLE_TOKEN
import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowAccessApi
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.JsonCadenceBuilder
import com.nftco.flow.sdk.crypto.Crypto
import com.nftco.flow.sdk.simpleFlowTransaction
import com.rarible.blockchain.scanner.flow.service.SporkService
import com.rarible.core.test.containers.KGenericContainer
import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
import com.rarible.flow.log.Log
import com.rarible.flow.scanner.emulator.EmulatorUser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.core.io.Resource
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.utility.MountableFile

@IntegrationTest
@MongoTest
@MongoCleanup
class SoftCollectionIndexingTest {

    private lateinit var accessApi: FlowAccessApi

    @Value("classpath:emulator/transactions/soft-collection/create-minter.cdc")
    private lateinit var collectionMintTx: Resource

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
/*        private val flowEmulator: KGenericContainer = KGenericContainer(
            ImageFromDockerfile().withDockerfileFromBuilder { builder ->
                builder.from("ubuntu:latest")
                    .run("RUN apt-get update -y && apt-get install -y curl")
                    .run("sh -ci \"\$(curl -fsSL https://storage.googleapis.com/flow-cli/install.sh)\"")
                    .env("PATH", "/root/.local/bin:\$PATH")
                    .env("FLOW_BLOCKTIME", "1s")
                    .env("FLOW_WITHCONTRACTS", "true")
                    .env("FLOW_SERVICEPRIVATEKEY", EmulatorUser.Emulator.keyHex)
                    .env("FLOW_SERVICEPUBLICKEY", EmulatorUser.Emulator.pubHex)
                    .expose(3569, 8080, 8888, 8701)
                    .workDir("/root")
                    .cmd("flow emulator")
                    .build()
            }.dockerImageName
        ).withCopyFileToContainer(
            MountableFile.forClasspathResource("emulator/contracts"),
            "/root/contracts"
        )
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("emulator/flow.json"),
                "/root/flow.json"
            )
            .withLogConsumer {
                logger.info("EMU: ${it.utf8String}")
            }
            .withExposedPorts(3569, 8080)
            .withReuse(true)
            .waitingFor(Wait.forHttp("/").forPort(8080).forStatusCode(500))*/
        val flowEmulator: KGenericContainer = KGenericContainer(
            "zolt85/flow-cli-emulator:latest"
//            "rari:latest"
        )/*.withEnv("FLOW_BLOCKTIME", "1000ms").withEnv("FLOW_WITHCONTRACTS", "true")
            .withEnv("FLOW_SERVICEPRIVATEKEY", EmulatorUser.Emulator.keyHex)
            .withEnv("FLOW_SERVICEPUBLICKEY", EmulatorUser.Emulator.pubHex)
            .withEnv("FLOW_VERBOSE", "true")*/
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
            .withCommand("flow emulator --block-time=1s --contracts=true --service-priv-key=${EmulatorUser.Emulator.keyHex} --service-pub-key=${EmulatorUser.Emulator.pubHex} --verbose=true")
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
    internal fun emulatorHealthTest() {
        accessApi.ping()
    }

    @Test
    internal fun mintCollectionIndexingTest() {
        val payerKey = accessApi.getAccountAtLatestBlock(EmulatorUser.Squidward.address)!!.keys[0]
        val signer = Crypto.getSigner(
            privateKey = Crypto.decodePrivateKey(EmulatorUser.Squidward.keyHex),
            hashAlgo = payerKey.hashAlgo
        )
        val cb = JsonCadenceBuilder()
        val tx = accessApi.simpleFlowTransaction(address = EmulatorUser.Squidward.address, signer = signer) {
            script(collectionMintTx.inputStream.bufferedReader().use { it.readText() }, FlowChainId.EMULATOR)
            argument(cb.address(EmulatorUser.Squidward.address.formatted))
            argument(cb.optional(null))
            argument(cb.string("Awesome Collection"))
            argument(cb.string("AC"))
            argument(cb.string("Some icon"))
            argument(cb.string("Description"))
            argument(cb.string("https://google.com/"))
            argument(cb.optional(null))
            argument(cb.array { emptyList() })
        }.sendAndWaitForSeal()
        Assertions.assertNull(tx)
        Assertions.assertTrue(tx.events.isNotEmpty())
    }
}
