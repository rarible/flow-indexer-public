package com.rarible.flow.scanner.activity.nft

import com.rarible.blockchain.scanner.flow.repository.FlowLogRepository
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.config.FlowListenerProperties
import org.springframework.stereotype.Component

@Component
class GamisodesActivityMaker(
    flowLogRepository: FlowLogRepository,
    txManager: TxManager,
    properties: FlowListenerProperties,
) : NFTActivityMaker(flowLogRepository, txManager, properties) {

    override fun meta(logEvent: FlowLogEvent): Map<String, String> = emptyMap()

    override val contractName: String = Contracts.GAMISODES.contractName
}
