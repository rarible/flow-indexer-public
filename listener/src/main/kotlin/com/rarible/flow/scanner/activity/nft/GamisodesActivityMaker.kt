package com.rarible.flow.scanner.activity.nft

import com.rarible.blockchain.scanner.flow.repository.FlowLogRepository
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.FlowLogType
import com.rarible.flow.core.domain.TransferActivity
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.config.FlowListenerProperties
import com.rarible.flow.scanner.model.WithdrawEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class GamisodesActivityMaker(
    flowLogRepository: FlowLogRepository,
    txManager: TxManager,
    properties: FlowListenerProperties,
) : NFTActivityMaker(flowLogRepository, txManager, properties) {

    override fun meta(logEvent: FlowLogEvent): Map<String, String> = emptyMap()

    override val contractName: String = Contracts.GAMISODES.contractName

    override suspend fun getDepositTransferActivity(event: FlowLogEvent): TransferActivity? {
        val deposit = deposit(event)
        val events = findBefore(deposit, listOf(FlowLogType.WITHDRAW, FlowLogType.MINT))
        if (events.size > 1) {
            logger.info(
                "Found more then 1 event coupled with deposit: tx=${event.log.transactionHash}, index=${event.log.eventIndex}, get last ${events.firstOrNull()?.eventIndex}"
            )
        }
        val withdraw = when (val nftEvent = events.firstOrNull()) {
            is WithdrawEvent -> nftEvent
            null -> null
            else -> return null
        }
        return if (deposit.optionalTo != null) {
            TransferActivity(
                contract = deposit.collection,
                tokenId = deposit.tokenId,
                to = deposit.to,
                from = withdraw?.from ?: deposit.contractAddress,
                timestamp = event.log.timestamp
            )
        } else null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GamisodesActivityMaker::class.java)
    }
}
