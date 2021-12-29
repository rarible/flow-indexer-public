package com.rarible.flow.scanner.job

import com.nftco.flow.sdk.*
import com.nftco.flow.sdk.cadence.JsonCadenceBuilder
import com.nftco.flow.sdk.crypto.Crypto
import com.rarible.flow.core.domain.AuctionStatus
import com.rarible.flow.core.domain.EnglishAuctionLot
import com.rarible.flow.core.repository.EnglishAuctionLotRepository
import com.rarible.flow.log.Log
import com.rarible.flow.scanner.config.FlowApiProperties
import kotlinx.coroutines.future.await
import kotlinx.coroutines.reactive.collect
import kotlinx.coroutines.runBlocking
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

@Component
class HammerLotOnFinishTimeJob(
    private val repo: EnglishAuctionLotRepository,
    private val api: AsyncFlowAccessApi,
    private val syncApi: FlowAccessApi,
    private val flowApiProperties: FlowApiProperties
) {

    private val logger by Log()

    private val completeScript: FlowScript by lazy {
        FlowScript(
            Flow.DEFAULT_ADDRESS_REGISTRY.processScript(
        """
            import EnglishAuction from 0xENGLISH_AUCTION
            
            transaction (lotId: UInt64) {
                prepare(account: AuthAccount) {}
    
                execute {
                    EnglishAuction.borrowAuction().completeLot(lotId: lotId)
                }
            }
        """.trimIndent(), chainId = flowApiProperties.chainId
            )
        )
    }

    private val pKey =
        Crypto.decodePrivateKey(flowApiProperties.serviceAccount.privateKey)

    private val pAddress = flowApiProperties.serviceAccount.address

    private val cadenceBuilder: JsonCadenceBuilder = JsonCadenceBuilder()

    @PostConstruct
    fun postCreate() {
        Flow.DEFAULT_ADDRESS_REGISTRY.register("0xENGLISH_AUCTION", pAddress, FlowChainId.TESTNET) //todo config mainnet
    }

    @Scheduled(fixedDelay = 60L, timeUnit = TimeUnit.SECONDS)
    fun hammerLots() = runBlocking {
        logger.info("Try to hammer finished lots ...")
        repo.findAllByStatusAndFinishAtLessThanEqualAndLastBidIsNotNull(
            status = AuctionStatus.ACTIVE,
            finishAt = Instant.now()
        ).collect {
            val txId = executeTx(it)
            logger.info("Lot [${it.id}] completed at tx: $txId")
        }
        logger.info("All finished lot's are hammered ...")
    }

    private suspend fun executeTx(lot: EnglishAuctionLot): FlowId {
        try {
            val lastBlockId = api.getLatestBlockHeader().await().id
            val acc = api.getAccountAtLatestBlock(pAddress).await()!!
            var tx = FlowTransaction(
                script = completeScript,
                arguments = listOf(FlowArgument(cadenceBuilder.uint64(lot.id))),
                referenceBlockId = lastBlockId,
                gasLimit = 1000L,
                proposalKey = FlowTransactionProposalKey(
                    address = pAddress,
                    keyIndex = acc.keys[0].id,
                    sequenceNumber = acc.keys[0].sequenceNumber.toLong()
                ),
                payerAddress = pAddress,
                authorizers = listOf(pAddress)
            )

            val signer = Crypto.getSigner(privateKey = pKey, acc.keys[0].hashAlgo)
            tx = tx.addEnvelopeSignature(address = pAddress, keyIndex = acc.keys[0].id, signer = signer)
            val txId = api.sendTransaction(tx).await()
            val result = waitForSeal(syncApi, txId, timeoutMs = 30_000L)
            if (result.errorMessage.isNotEmpty()) {
                throw IllegalStateException("Error while execute transaction [$txId]: ${result.errorMessage}")
            }
            return txId
        } catch (e: Exception) {
            logger.error("HammerLotOnFinishTimeJob::executeTx failed! ${e.message}", e)
            throw e
        }
    }
}
