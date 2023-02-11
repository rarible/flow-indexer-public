package com.rarible.flow.scanner.job

import com.nftco.flow.sdk.*
import com.nftco.flow.sdk.cadence.JsonCadenceBuilder
import com.nftco.flow.sdk.crypto.Crypto
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.AuctionStatus
import com.rarible.flow.core.domain.EnglishAuctionLot
import com.rarible.flow.core.util.Log
import com.rarible.flow.scanner.config.FlowApiProperties
import kotlinx.coroutines.future.await
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

@Component
class HammerLotOnFinishTimeJob(
    private val mongo: ReactiveMongoTemplate,
    private val api: AsyncFlowAccessApi,
    private val syncApi: FlowAccessApi,
    private val flowApiProperties: FlowApiProperties
) {

    private val logger by Log()

    private val completeScript: FlowScript by lazy {
        FlowScript(
            Flow.DEFAULT_ADDRESS_REGISTRY.processScript(
        """
            import EnglishAuction from 0xENGLISHAUCTION

            transaction(auctionId: UInt64) {
                prepare(account: AuthAccount) {}
                execute {
                    EnglishAuction.completeLot(auctionId: auctionId)
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
        Contracts.ENGLISH_AUCTION.register(Flow.DEFAULT_ADDRESS_REGISTRY)
    }


    @Scheduled(initialDelay = 40L, fixedDelay = 30L, timeUnit = TimeUnit.SECONDS)
    fun hammerLots() {
        runBlocking {
            try {
                logger.info("Try to hammer finished lots ...")
                mongo.find(
                    Query.query(
                        where(EnglishAuctionLot::status).isEqualTo(AuctionStatus.ACTIVE)
                            .and(EnglishAuctionLot::finishAt).exists(true).lte(Instant.now())
                    ),
                    EnglishAuctionLot::class.java
                ).asFlow().collect {
                    val (txId, txResult) = executeTx(it)

                    if (txResult.errorMessage.isNotEmpty()) {
                        logger.error("Failed to hammer lot [${it.id}]: ${txResult.errorMessage}")
                    } else {
                        logger.info("Tx for hammer lot [${it.id}]: ${txId}]")
                    }
                }
                logger.info("All finished lot's are hammered ...")
            } catch (e: Exception) {
                logger.error("HammerLotOnFinishTimeJob failed! ${e.message}", e)
            }
        }
    }

    private suspend fun executeTx(lot: EnglishAuctionLot): Pair<FlowId, FlowTransactionResult> {
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
        return txId to waitForSeal(syncApi, txId, timeoutMs = 30_000L)
    }
}
