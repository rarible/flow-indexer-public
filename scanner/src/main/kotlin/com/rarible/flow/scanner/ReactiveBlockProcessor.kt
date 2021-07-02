package com.rarible.flow.scanner

import com.rarible.flow.log.Log
import com.rarible.flow.scanner.model.FlowBlock
import com.rarible.flow.scanner.model.FlowEvent
import com.rarible.flow.scanner.model.FlowTransaction
import com.rarible.flow.scanner.repo.FlowBlockRepository
import com.rarible.flow.scanner.repo.FlowTransactionRepository
import org.bouncycastle.util.encoders.Hex
import org.onflow.protobuf.access.Access
import org.onflow.protobuf.access.AccessAPIGrpc
import org.onflow.sdk.asLocalDateTime
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toMono
import java.util.*
import javax.annotation.PreDestroy

/**
 * Created by TimochkinEA at 17.06.2021
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class ReactiveBlockProcessor(
    private val blockRepository: FlowBlockRepository,
    private val txRepository: FlowTransactionRepository,
    private val analyzer: IFlowEventAnalyzer
) {

    private val schedulerP = Schedulers.newBoundedElastic(256, 256, UUID.randomUUID().toString())
    private val schedulerS = Schedulers.newBoundedElastic(256, 256, UUID.randomUUID().toString())

    private val log by Log()

    /**
     * Read list of blocks (sealed)
     *
     * @param client    Access API Client
     * @param ids       list of block's height for read
     */
    fun doRead(client: AccessAPIGrpc.AccessAPIBlockingStub, ids: List<Long>) {
        log.info("Do read ${ids.size} ids from chain!")
        Flux.fromIterable(ids)
            .map { id ->
                log.info("\tBlock with height received!")
                client.getBlockByHeight(Access.GetBlockByHeightRequest.newBuilder().setHeight(id).build())
            }
            .flatMap { blockResponse ->
                val block = blockResponse.block
                log.info("\tRead transactions for block [${block.height}]")
                val fb = FlowBlock(
                    id = Hex.toHexString(block.id.toByteArray()),
                    parentId = Hex.toHexString(block.parentId.toByteArray()),
                    height = block.height,
                    timestamp = block.timestamp.asLocalDateTime(),
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
                                id = UUID.randomUUID().toString().replace("-", "").lowercase(),
                                referenceBlockId = Hex.toHexString(tx.referenceBlockId.toByteArray()),
                                blockHeight = fb.height,
                                proposer = Hex.toHexString(tx.proposalKey.address.toByteArray()),
                                payer = Hex.toHexString(tx.payer.toByteArray()),
                                authorizers = tx.authorizersList.map { Hex.toHexString(it.toByteArray()) },
                                script = tx.script.toStringUtf8().trimIndent(),
                                events = result.eventsList.map {
                                    FlowEvent(
                                        type = it.type,
                                        data = it.payload.toStringUtf8(),
                                        timestamp = fb.timestamp
                                    )
                                }
                            ))
                        }
                    }
                }
                fb.transactionsCount = transactions.size
                log.info("Block prepared to save!")
                Pair(fb, transactions.toList()).toMono()
            }
            .publishOn(schedulerP)
            .subscribeOn(schedulerS)
            .retry(10L)
            .subscribe(::process)

    }

    private fun process(blockInfo: Pair<FlowBlock, List<FlowTransaction>>) {
        log.info("Save Block [${blockInfo.first}]")
        blockRepository.save(blockInfo.first).subscribe()
        if (blockInfo.second.isNotEmpty()) {
            txRepository.saveAll(blockInfo.second).subscribe { tx ->
                analyzer.analyze(tx)
            }
        }
    }

    @PreDestroy
    fun preDestroy() {
        schedulerP.dispose()
        schedulerS.dispose()
    }
}
