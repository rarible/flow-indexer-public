package com.rarible.flow.scanner

import com.nftco.flow.sdk.FlowId
import com.nftco.flow.sdk.FlowTransactionResult
import com.rarible.blockchain.scanner.flow.service.SporkService
import kotlinx.coroutines.future.await
import org.springframework.stereotype.Component

@Component
class TxManager(val sporkService: SporkService) {
    suspend fun <T> onTransaction(blockHeight: Long, transactionId: FlowId, block: (FlowTransactionResult) -> T): T {
        val api = sporkService.spork(blockHeight).api
        val transactionResult = api.getTransactionResultById(transactionId).await()!!
        return block(transactionResult)
    }
}
