package com.rarible.flow.scanner

import com.rarible.flow.scanner.model.FlowBlock
import com.rarible.flow.scanner.model.FlowEvent
import com.rarible.flow.scanner.model.FlowTransaction
import com.rarible.flow.scanner.repo.FlowBlockRepository
import com.rarible.flow.scanner.repo.FlowTransactionRepository
import net.devh.boot.grpc.client.inject.GrpcClient
import org.bouncycastle.util.encoders.Hex
import org.onflow.protobuf.access.Access
import org.onflow.protobuf.access.AccessAPIGrpc
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.time.Instant
import java.util.concurrent.Executors
import javax.annotation.PreDestroy

/**
 * Created by TimochkinEA at 17.06.2021
 */
@Component
class ReactiveBlockProcessor(
    private val blockRepository: FlowBlockRepository,
    private val txRepository: FlowTransactionRepository,
    private val analyzer: FlowEventAnalyzer
) {

    private val log: Logger = LoggerFactory.getLogger(ReactiveBlockProcessor::class.java)

    @GrpcClient("flow")
    private lateinit var client: AccessAPIGrpc.AccessAPIBlockingStub

    private val schedulerP = Schedulers.fromExecutor(Executors.newCachedThreadPool())
    private val schedulerS = Schedulers.fromExecutor(Executors.newCachedThreadPool())

    fun doRead(ids: List<Long>) {
        log.info("Do read ${ids.size} ids from chain!")
        Flux.fromIterable(ids)
            .map { id ->
                log.info("\tBlock with height received!")
                client.getBlockByHeight(Access.GetBlockByHeightRequest.newBuilder().setHeight(id).build())
            }
            .map { blockResponse ->

                val block = blockResponse.block
                log.info("\tRead transactions for block [${block.height}]")
                val fb = FlowBlock(
                    id = Hex.toHexString(block.id.toByteArray()),
                    parentId = Hex.toHexString(block.parentId.toByteArray()),
                    height = block.height,
                    timestamp = Instant.ofEpochSecond(block.timestamp.seconds),
                    collectionsCount = block.collectionGuaranteesCount
                )

                val transactions = mutableListOf<FlowTransaction>()
                if (fb.collectionsCount > 0) {
                    block.collectionGuaranteesList.forEach { cgl ->
                        client.getCollectionByID(
                            Access.GetCollectionByIDRequest.newBuilder().setId(cgl.collectionId).build()
                        ).collection.transactionIdsList.forEach { txId ->
                            val request = Access.GetTransactionRequest.newBuilder().setId(txId).build()
                            val tx = client.getTransaction(request).transaction
                            val result = client.getTransactionResult(request)

                            transactions.add(FlowTransaction(
                                id = Hex.toHexString(tx.referenceBlockId.toByteArray()),
                                blockHeight = fb.height,
                                proposer = Hex.toHexString(tx.proposalKey.address.toByteArray()),
                                payer = Hex.toHexString(tx.payer.toByteArray()),
                                authorizers = tx.authorizersList.map { Hex.toHexString(it.toByteArray()) },
                                script = tx.script.toStringUtf8().trimIndent(),
                                events = result.eventsList.map {
                                    FlowEvent(
                                        type = it.type,
                                        data = it.payload.toStringUtf8()
                                    )
                                }
                            ))
                        }
                    }
                }
                fb.transactionsCount = transactions.size
                log.info("Block prepared to save!")
                Pair(fb, transactions.toList())

            }
            .publishOn(schedulerP)
            .subscribeOn(schedulerS)
            .retry(10L)
            .subscribe { pair ->
                log.info("Save Block [${pair.first}]")
                blockRepository.save(pair.first).subscribe()
                if (pair.second.isNotEmpty()) {
                    txRepository.saveAll(pair.second).subscribe { tx ->
                        analyzer.analyze(tx)
                    }
                }
            }
    }

    @PreDestroy
    fun preDestroy() {
        schedulerP.dispose()
        schedulerS.dispose()
    }
}
