package com.rarible.flow.scanner.activity.disabled

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.repository.FlowLogRepository
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.activity.nft.NFTActivityMaker

class CnnActivity(
    flowLogRepository: FlowLogRepository,
    txManager: TxManager,
    chainId: FlowChainId,
) : NFTActivityMaker(flowLogRepository, txManager, chainId) {

    override val contractName: String = Contracts.CNN.contractName

    override fun tokenId(logEvent: FlowLogEvent): Long = mint(logEvent).tokenId

    override fun meta(logEvent: FlowLogEvent): Map<String, String> = emptyMap()

    override fun royalties(logEvent: FlowLogEvent): List<Part> {
        return Contracts.CNN.staticRoyalties(chainId)
    }
}
