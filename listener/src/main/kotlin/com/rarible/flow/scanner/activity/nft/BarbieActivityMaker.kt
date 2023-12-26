package com.rarible.flow.scanner.activity.nft

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.repository.FlowLogRepository
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.TxManager
import org.springframework.stereotype.Component

sealed class BarbieActivityMaker(
    flowLogRepository: FlowLogRepository,
    txManager: TxManager,
    chainId: FlowChainId,
) : NFTActivityMaker(flowLogRepository, txManager, chainId) {

    override fun meta(logEvent: FlowLogEvent): Map<String, String> = emptyMap()

    override fun royalties(logEvent: FlowLogEvent): List<Part> {
        return Contracts.BARBIE_CARD.staticRoyalties(chainId)
    }
}

@Component
class BarbieCardActivity(
    logRepository: FlowLogRepository,
    txManager: TxManager,
    chainId: FlowChainId,
) : BarbieActivityMaker(logRepository, txManager, chainId) {

    override val contractName: String = Contracts.BARBIE_CARD.contractName
}

@Component
class BarbiePackActivity(
    logRepository: FlowLogRepository,
    txManager: TxManager,
    chainId: FlowChainId,
) : BarbieActivityMaker(logRepository, txManager, chainId) {

    override val contractName: String = Contracts.BARBIE_PACK.contractName
}

@Component
class BarbieTokenActivity(
    logRepository: FlowLogRepository,
    txManager: TxManager,
    chainId: FlowChainId,
) : BarbieActivityMaker(logRepository, txManager, chainId) {

    override val contractName: String = Contracts.BARBIE_TOKEN.contractName
}

@Component
class RaribleBarbieCardActivity(
    logRepository: FlowLogRepository,
    txManager: TxManager,
    chainId: FlowChainId,
) : BarbieActivityMaker(logRepository, txManager, chainId) {

    override val contractName: String = Contracts.RARIBLE_BARBIE_CARD.contractName
}

@Component
class RaribleBarbiePackActivity(
    logRepository: FlowLogRepository,
    txManager: TxManager,
    chainId: FlowChainId,
) : BarbieActivityMaker(logRepository, txManager, chainId) {

    override val contractName: String = Contracts.RARIBLE_BARBIE_PACK.contractName
}

@Component
class RaribleBarbieTokenActivity(
    logRepository: FlowLogRepository,
    txManager: TxManager,
    chainId: FlowChainId,
) : BarbieActivityMaker(logRepository, txManager, chainId) {

    override val contractName: String = Contracts.RARIBLE_BARBIE_TOKEN.contractName
}
