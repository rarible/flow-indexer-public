package com.rarible.flow.scanner.activity.nft

import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.config.FlowListenerProperties
import org.springframework.stereotype.Component

sealed class BaseHWActivityMaker(
    private val config: FlowListenerProperties
) : NFTActivityMaker() {

    override fun tokenId(logEvent: FlowLogEvent): Long = cadenceParser.long(logEvent.event.fields["id"]!!)

    override fun meta(logEvent: FlowLogEvent): Map<String, String> = emptyMap()

    override fun royalties(logEvent: FlowLogEvent): List<Part> {
        return Contracts.HW_GARAGE_CARD.staticRoyalties(config.chainId)
    }
}

@Component
class HWCardActivity(config: FlowListenerProperties) : BaseHWActivityMaker(config) {
    override val contractName: String = Contracts.HW_GARAGE_CARD.contractName
}

@Component
class HWPackActivity(config: FlowListenerProperties) : BaseHWActivityMaker(config) {
    override val contractName: String = Contracts.HW_GARAGE_PACK.contractName
}

@Component
class RaribleCardActivity(config: FlowListenerProperties) : BaseHWActivityMaker(config) {
    override val contractName: String = Contracts.RARIBLE_GARAGE_CARD.contractName
}

@Component
class RariblePackActivity(config: FlowListenerProperties) : BaseHWActivityMaker(config) {
    override val contractName: String = Contracts.RARIBLE_GARAGE_PACK.contractName
}
