package com.rarible.flow.scanner

import com.nftco.flow.sdk.*
import com.rarible.blockchain.scanner.flow.service.SporkService
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class TxManager(val sporkService: SporkService) {
    @PostConstruct
    fun pc() {
        sporkService.allSporks[FlowChainId.EMULATOR] = listOf(
            SporkService.Spork(from = 0L, nodeUrl = "localhost", port = 3569),
        )
    }

    fun <T> onTransaction(transactionId: FlowId, block: (FlowTransactionResult) -> T): T {
        val api = sporkService.sporkForTx(transactionId).api
        val transactionResult = api.getTransactionResultById(transactionId).get()!!
        return block(transactionResult)
    }
}
