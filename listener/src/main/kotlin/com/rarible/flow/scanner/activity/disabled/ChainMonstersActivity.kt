package com.rarible.flow.scanner.activity.disabled

import com.nftco.flow.sdk.cadence.UInt32NumberField
import com.rarible.blockchain.scanner.flow.repository.FlowLogRepository
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.activity.nft.NFTActivityMaker
import com.rarible.flow.scanner.config.FlowListenerProperties
import com.rarible.flow.scanner.model.BurnEvent
import com.rarible.flow.scanner.model.ChainMonstersBurnEvent
import com.rarible.flow.scanner.model.ChainMonstersDepositEvent
import com.rarible.flow.scanner.model.ChainMonstersMintEvent
import com.rarible.flow.scanner.model.ChainMonstersWithdrawEvent
import com.rarible.flow.scanner.model.DepositEvent
import com.rarible.flow.scanner.model.MintEvent
import com.rarible.flow.scanner.model.WithdrawEvent
import kotlin.math.min

class ChainMonstersActivity(
    flowLogRepository: FlowLogRepository,
    txManager: TxManager,
    properties: FlowListenerProperties,
) : NFTActivityMaker(flowLogRepository, txManager, properties) {

    override val contractName: String = Contracts.CHAINMONSTERS.contractName

    override fun tokenId(logEvent: FlowLogEvent): Long = mint(logEvent).tokenId

    override fun meta(logEvent: FlowLogEvent): Map<String, String> {
        val rewardID: UInt32NumberField by logEvent.event.fields
        val serialNumber: UInt32NumberField by logEvent.event.fields

        return mapOf(
            "rewardId" to rewardID.value!!,
            "serialNumber" to serialNumber.value!!
        )
    }

    override fun mint(logEvent: FlowLogEvent): MintEvent {
        return ChainMonstersMintEvent(logEvent)
    }

    override fun burn(logEvent: FlowLogEvent): BurnEvent {
        return ChainMonstersBurnEvent(logEvent)
    }

    override fun deposit(logEvent: FlowLogEvent): DepositEvent {
        return ChainMonstersDepositEvent(logEvent)
    }

    override fun withdraw(logEvent: FlowLogEvent): WithdrawEvent {
        return ChainMonstersWithdrawEvent(logEvent)
    }

    override fun royalties(logEvent: FlowLogEvent): List<Part> {
        return Contracts.CHAINMONSTERS.staticRoyalties(chainId)
    }
}
