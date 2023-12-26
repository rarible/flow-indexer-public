package com.rarible.flow.scanner.activity.disabled

import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.StringField
import com.rarible.blockchain.scanner.flow.repository.FlowLogRepository
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.activity.nft.NFTActivityMaker

class StarlyActivity(
    flowLogRepository: FlowLogRepository,
    txManager: TxManager,
    chainId: FlowChainId,
) : NFTActivityMaker(flowLogRepository, txManager, chainId) {

    override val contractName: String = Contracts.STARLY_CARD.contractName

    override fun tokenId(logEvent: FlowLogEvent): Long = mint(logEvent).tokenId

    override fun meta(logEvent: FlowLogEvent): Map<String, String> {
        val starlyID: StringField by logEvent.event.fields
        return mapOf(
            "starlyId" to starlyID.value!!,
        )
    }

    override fun royalties(logEvent: FlowLogEvent): List<Part> {
        return Contracts.STARLY_CARD.staticRoyalties(chainId)
    }
}
