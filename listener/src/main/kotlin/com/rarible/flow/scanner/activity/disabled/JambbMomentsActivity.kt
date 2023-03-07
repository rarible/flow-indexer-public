package com.rarible.flow.scanner.activity.disabled

import com.nftco.flow.sdk.cadence.UInt64NumberField
import com.rarible.blockchain.scanner.flow.repository.FlowLogRepository
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.activity.nft.NFTActivityMaker
import com.rarible.flow.scanner.config.FlowListenerProperties
import com.rarible.flow.scanner.model.BurnEvent
import com.rarible.flow.scanner.model.DepositEvent
import com.rarible.flow.scanner.model.JambbMomentsBurnEvent
import com.rarible.flow.scanner.model.JambbMomentsDepositEvent
import com.rarible.flow.scanner.model.JambbMomentsMintEvent
import com.rarible.flow.scanner.model.JambbMomentsWithdrawEvent
import com.rarible.flow.scanner.model.MintEvent
import com.rarible.flow.scanner.model.WithdrawEvent

class JambbMomentsActivity(
    flowLogRepository: FlowLogRepository,
    txManager: TxManager,
    properties: FlowListenerProperties,
): NFTActivityMaker(flowLogRepository, txManager, properties) {

    override val contractName: String = Contracts.JAMBB_MOMENTS.contractName

    override fun tokenId(logEvent: FlowLogEvent): Long = mint(logEvent).tokenId

    override fun meta(logEvent: FlowLogEvent): Map<String, String> {
        val momentID: UInt64NumberField by logEvent.event.fields
        val contentID: UInt64NumberField by logEvent.event.fields
        val contentEditionID: UInt64NumberField by logEvent.event.fields
        val serialNumber: UInt64NumberField by logEvent.event.fields
        val seriesID: UInt64NumberField by logEvent.event.fields
        val setID: UInt64NumberField by logEvent.event.fields

        return mapOf(
            "momentID" to momentID.value!!,
            "contentID" to contentID.value!!,
            "contentEditionID" to contentEditionID.value!!,
            "serialNumber" to serialNumber.value!!,
            "seriesID" to seriesID.value!!,
            "setID" to setID.value!!,
        )
    }

    override fun mint(logEvent: FlowLogEvent): MintEvent {
        return JambbMomentsMintEvent(logEvent)
    }

    override fun burn(logEvent: FlowLogEvent): BurnEvent {
        return JambbMomentsBurnEvent(logEvent)
    }

    override fun deposit(logEvent: FlowLogEvent): DepositEvent {
        return JambbMomentsDepositEvent(logEvent)
    }

    override fun withdraw(logEvent: FlowLogEvent): WithdrawEvent {
        return JambbMomentsWithdrawEvent(logEvent)
    }

    override fun royalties(logEvent: FlowLogEvent): List<Part> {
        return Contracts.JAMBB_MOMENTS.staticRoyalties(chainId)
    }
}
