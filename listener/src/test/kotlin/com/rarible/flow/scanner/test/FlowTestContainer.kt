package com.rarible.flow.scanner.test

import com.nftco.flow.sdk.AddressRegistry
import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowAccessApi
import com.nftco.flow.sdk.FlowAccountKey
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.FlowTransactionResult
import com.nftco.flow.sdk.FlowTransactionStatus
import com.nftco.flow.sdk.Signer
import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.simpleFlowTransaction
import com.rarible.core.test.containers.KGenericContainer
import com.rarible.flow.scanner.test.contract.RaribleNFTTestContract
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.LoggerFactory
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.MountableFile

object FlowTestContainer {

    private val logger = LoggerFactory.getLogger(FlowTestContainer::class.java)

    private const val FLOW_IMAGE_NAME = "zolt85/flow-cli-emulator:latest"

    private val pathContracts = getMountablePath("emulator/contracts")
    private val pathFlowConfig = getMountablePath("emulator/flow.json")

    private val flowEmulator: KGenericContainer
    private val accessApi: FlowAccessApi

    init {
        val start = System.currentTimeMillis()
        logger.info("===== SETUP FLOW - START =====")

        flowEmulator = KGenericContainer(FLOW_IMAGE_NAME)
            .withEnv("FLOW_BLOCKTIME", "1s")
            .withEnv("FLOW_WITHCONTRACTS", "true")
            .withCopyFileToContainer(pathFlowConfig, "/root/flow.json")
            .withCopyFileToContainer(pathContracts, "/root/contracts")
            .withExposedPorts(3569, 8080)
            .withLogConsumer { logger.info("EMU: ${it.utf8String}") }
            .withReuse(true)
            .withCommand("flow emulator")
            .waitingFor(Wait.forHttp("/").forPort(8080).forStatusCode(500))

        flowEmulator.start()

        logger.info("Flow emulator is up!")
        logger.info(flowEmulator.execInContainer("flow", "project", "deploy").stdout)

        accessApi = Flow.newAccessApi(host(), port())

        createAccount(EmulatorUser.Patrick.pubHex)
        createAccount(EmulatorUser.Squidward.pubHex)
        createAccount(EmulatorUser.Gary.pubHex)

        createContract(AddressRegistry.NON_FUNGIBLE_TOKEN)
        initContracts()

        logger.info("===== SETUP FLOW - FINISH ({}ms) =====", start - System.currentTimeMillis())
    }

    fun createAccount(address: String) {
        val result = flowEmulator.execInContainer(
            "flow",
            "accounts",
            "create",
            "--key",
            address
        )
        logger.info(result.stdout)
    }

    fun createContract(contract: String, address: FlowAddress = EmulatorUser.Emulator.address) {
        Flow.DEFAULT_ADDRESS_REGISTRY.register(contract, address, FlowChainId.EMULATOR)
        logger.info("Contract {} registered for {}", contract, address.formatted)
    }

    fun getAccountKey(address: FlowAddress): FlowAccountKey {
        val account = accessApi.getAccountAtLatestBlock(address)
            ?: throw IllegalArgumentException("Account ${address.formatted} found for")
        return account.keys[0]
    }

    fun execute(
        address: FlowAddress,
        signer: Signer,
        scriptPath: String,
        vararg args: Field<*>
    ): FlowTransactionResult {
        val scriptText = this.javaClass.getResource(scriptPath)!!.readText()
        val tx = accessApi.simpleFlowTransaction(
            address = address,
            signer = signer
        ) {
            script(scriptText, FlowChainId.EMULATOR)
            args.forEach { argument(it) }
        }.sendAndWaitForSeal()

        if (tx.errorMessage.isNotEmpty()) {
            throw IllegalArgumentException("Failed to execute script '$scriptPath', error: ${tx.errorMessage}")
        }

        assertThat(tx.status).isEqualTo(FlowTransactionStatus.SEALED)

        logger.info(
            "Result of transaction (address={}, signer={}, scriptPath={}, args={}): {}",
            address, signer, scriptPath, args, tx
        )

        return tx
    }

    fun host(): String = flowEmulator.host

    fun port(): Int = flowEmulator.getMappedPort(3569)

    fun ping() {
        logger.info("Flow emulator launched at: {}:{}", host(), port())
    }

    private fun getMountablePath(testDataPath: String): MountableFile {
        return MountableFile.forClasspathResource(testDataPath)
    }

    private fun initContracts() {
        RaribleNFTTestContract.init()
    }
}
