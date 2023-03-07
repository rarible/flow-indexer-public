package com.rarible.flow.scanner.activity.nft

import com.rarible.blockchain.scanner.flow.repository.FlowLogRepository
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.config.FlowListenerProperties
import org.springframework.stereotype.Component

sealed class HWActivityMaker(
    flowLogRepository: FlowLogRepository,
    txManager: TxManager,
    properties: FlowListenerProperties,
) : NFTActivityMaker(flowLogRepository, txManager, properties) {

    override fun tokenId(logEvent: FlowLogEvent): Long = cadenceParser.long(logEvent.event.fields["id"]!!)

    override fun meta(logEvent: FlowLogEvent): Map<String, String> = emptyMap()

    override fun royalties(logEvent: FlowLogEvent): List<Part> {
        return Contracts.HW_GARAGE_CARD.staticRoyalties(chainId)
    }
}

@Component
class HWCardActivity(
    logRepository: FlowLogRepository,
    txManager: TxManager,
    properties: FlowListenerProperties,
) : HWActivityMaker(logRepository, txManager, properties) {
    override val contractName: String = Contracts.HW_GARAGE_CARD.contractName
}

@Component
class HWPackActivity(
    logRepository: FlowLogRepository,
    txManager: TxManager,
    properties: FlowListenerProperties,
) : HWActivityMaker(logRepository, txManager, properties) {
    override val contractName: String = Contracts.HW_GARAGE_PACK.contractName
}

@Component
class RaribleCardActivity(
    logRepository: FlowLogRepository,
    txManager: TxManager,
    properties: FlowListenerProperties,
) : HWActivityMaker(logRepository, txManager, properties) {
    override val contractName: String = Contracts.RARIBLE_GARAGE_CARD.contractName
}

@Component
class RariblePackActivity(
    logRepository: FlowLogRepository,
    txManager: TxManager,
    properties: FlowListenerProperties,
) : HWActivityMaker(logRepository, txManager, properties) {
    override val contractName: String = Contracts.RARIBLE_GARAGE_PACK.contractName
}