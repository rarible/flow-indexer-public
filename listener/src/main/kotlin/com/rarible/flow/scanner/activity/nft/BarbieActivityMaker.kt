package com.rarible.flow.scanner.activity.nft

import com.rarible.blockchain.scanner.flow.repository.FlowLogRepository
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.config.FlowListenerProperties
import org.springframework.stereotype.Component

sealed class BarbieActivityMaker(
    flowLogRepository: FlowLogRepository,
    txManager: TxManager,
    properties: FlowListenerProperties,
) : NFTActivityMaker(flowLogRepository, txManager, properties) {

    override fun meta(logEvent: FlowLogEvent): Map<String, String> = emptyMap()

    override fun royalties(logEvent: FlowLogEvent): List<Part> {
        return Contracts.BARBIE_CARD.staticRoyalties(chainId)
    }
}

@Component
class BarbieCardActivity(
    logRepository: FlowLogRepository,
    txManager: TxManager,
    properties: FlowListenerProperties,
) : BarbieActivityMaker(logRepository, txManager, properties) {

    override val contractName: String = Contracts.BARBIE_CARD.contractName
}

@Component
class BarbiePackActivity(
    logRepository: FlowLogRepository,
    txManager: TxManager,
    properties: FlowListenerProperties,
) : BarbieActivityMaker(logRepository, txManager, properties) {

    override val contractName: String = Contracts.BARBIE_PACK.contractName
}

@Component
class BarbieTokenActivity(
    logRepository: FlowLogRepository,
    txManager: TxManager,
    properties: FlowListenerProperties,
) : BarbieActivityMaker(logRepository, txManager, properties) {

    override val contractName: String = Contracts.BARBIE_TOKEN.contractName
}