package com.rarible.flow.scanner

import com.rarible.flow.log.Log
import com.rarible.flow.scanner.model.FlowBlock
import com.rarible.flow.scanner.model.FlowEvent
import com.rarible.flow.scanner.model.FlowTransaction
import com.rarible.flow.scanner.repo.FlowBlockRepository
import com.rarible.flow.scanner.repo.FlowTransactionRepository
import org.onflow.sdk.FlowAccessApi
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import java.util.*
import javax.annotation.PreDestroy

@Component
@Profile("!test")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class ReactiveBlockProcessor(
    private val blockRepository: FlowBlockRepository,
    private val txRepository: FlowTransactionRepository,
    private val analyzer: FlowTxAnalyzer,
    private val flowApi: FlowAccessApi
) {

    private val schedulerP = Schedulers.newBoundedElastic(256, 256, UUID.randomUUID().toString())
    private val schedulerS = Schedulers.newBoundedElastic(256, 256, UUID.randomUUID().toString())

    /**
     * Read list of blocks (sealed)
     *
     * @param heights       list of block's height for read
     */
    fun doRead(heights: List<Long>) {
        log.info("Do read ${heights.size} ids from chain!")
        heights.toFlux()
            .flatMap { height ->
                flowApi.getBlockByHeight(height).toMono()
            }.flatMap { block ->
                log.info("\tRead transactions for block [${block.height}]")
                val fb = FlowBlock(
                    id = block.id.base16Value,
                    parentId = block.parentId.base16Value,
                    height = block.height,
                    timestamp = block.timestamp,
                    collectionsCount = block.collectionGuarantees.size
                )
                val transactions = mutableListOf<FlowTransaction>()
                if (fb.collectionsCount > 0) {
                    block.collectionGuarantees.forEach { cgl ->
                        flowApi.getCollectionById(cgl.id)?.transactionIds?.forEach { txId ->
                            val tx = flowApi.getTransactionById(txId)
                            val result = flowApi.getTransactionResultById(txId)

                            if (tx != null && result != null) {
                                transactions.add(FlowTransaction(
                                    id = txId.base16Value,
                                    referenceBlockId = tx.referenceBlockId.base16Value,
                                    blockHeight = fb.height,
                                    proposer = tx.proposalKey.address.formatted,
                                    payer = tx.payerAddress.formatted,
                                    authorizers = tx.authorizers.map { it.formatted },
                                    script = tx.script.stringValue.trimIndent(),
                                    events = result.events.map {
                                        FlowEvent(
                                            type = it.type,
                                            data = it.payload.stringValue,
                                            timestamp = fb.timestamp
                                        )
                                    }
                                ))
                            }
                        }
                    }
                }
                fb.transactionsCount = transactions.size
                log.info("Block {} prepared to save!", block.height)
                Pair(fb, transactions.toList()).toMono()
            }
            .publishOn(schedulerP)
            .subscribeOn(schedulerS)
            .retry(10L)
            .subscribe(::process)

    }

    private fun process(blockInfo: Pair<FlowBlock, List<FlowTransaction>>) {
        log.info("Save Block [${blockInfo.first}]")
        blockRepository.save(blockInfo.first).subscribe {
            if (blockInfo.second.isNotEmpty()) {
                txRepository.saveAll(blockInfo.second).subscribe { tx ->
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

    companion object {
        private val log by Log()
    }
}
