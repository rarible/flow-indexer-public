package com.rarible.flow.scanner.activity.disabled

import com.rarible.flow.Contracts
import com.rarible.flow.core.config.AppProperties
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.activity.nft.NFTActivityMaker

class OneFootballActivity(
    private val appProperties: AppProperties
) : NFTActivityMaker() {

    override val contractName: String = Contracts.ONE_FOOTBALL.contractName

    override fun tokenId(logEvent: FlowLogEvent): Long = cadenceParser.long(logEvent.event.fields["id"]!!)

    override fun meta(logEvent: FlowLogEvent): Map<String, String> = emptyMap()

    override fun royalties(logEvent: FlowLogEvent): List<Part> {
        return Contracts.ONE_FOOTBALL.staticRoyalties(appProperties.chainId)
    }
}
