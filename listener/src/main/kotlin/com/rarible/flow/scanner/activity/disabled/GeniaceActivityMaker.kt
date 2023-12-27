package com.rarible.flow.scanner.activity.disabled

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.repository.FlowLogRepository
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.activity.nft.NFTActivityMaker

class GeniaceActivityMaker(
    flowLogRepository: FlowLogRepository,
    txManager: TxManager,
    chainId: FlowChainId,
) : NFTActivityMaker(flowLogRepository, txManager, chainId) {

    override val contractName = Contracts.GENIACE.contractName

    override fun tokenId(logEvent: FlowLogEvent) =
        cadenceParser.long(logEvent.event.fields["id"]!!)

    override fun meta(logEvent: FlowLogEvent): Map<String, String> = emptyMap()
}
