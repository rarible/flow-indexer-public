package com.rarible.flow.scanner

import com.nftco.flow.sdk.FlowEventResult
import com.nftco.flow.sdk.FlowId
import com.nftco.flow.sdk.FlowTransactionResult
import com.rarible.blockchain.scanner.flow.service.FlowApiFactory
import com.rarible.blockchain.scanner.flow.service.SporkService
import kotlinx.coroutines.future.await
import org.springframework.stereotype.Component

@Component
class TxManager(
    private val sporkService: SporkService,
    private val flowApiFactory: FlowApiFactory,
) {
    suspend fun <T> onTransaction(blockHeight: Long, transactionId: FlowId, block: (FlowTransactionResult) -> T): T {
        val api = flowApiFactory.getApi(sporkService.spork(blockHeight))
        val transactionResult = api.getTransactionResultById(transactionId).await()!!
        return block(transactionResult)
    }

    suspend fun getTransactionEvents(
        blockHeight: Long,
        transactionId: FlowId
    ): FlowTransactionResult {
        val api = flowApiFactory.getApi(sporkService.spork(blockHeight))
        return api.getTransactionResultById(transactionId).await()!!
    }

    suspend fun getTransactionEventByType(
        type: String,
        blockHeight: Long,
        transactionId: FlowId
    ): FlowEventResult {
        val api = flowApiFactory.getApi(sporkService.spork(blockHeight))
        val range = LongRange(blockHeight, blockHeight)
        val transactionResult = api.getEventsForHeightRange(type, range).await()
        return transactionResult.single()
    }
}
