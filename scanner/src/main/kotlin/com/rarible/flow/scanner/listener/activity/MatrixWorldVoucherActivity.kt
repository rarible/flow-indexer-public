package com.rarible.flow.scanner.listener.activity

import com.nftco.flow.sdk.cadence.StringField
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.scanner.listener.NFTActivityMaker
import org.springframework.stereotype.Component

@Component
class MatrixWorldVoucherActivity: NFTActivityMaker() {

    override val contractName: String = Contracts.MATRIX_WORLD_VOUCHER.contractName

    override fun tokenId(logEvent: FlowLogEvent): Long = cadenceParser.long(logEvent.event.fields["id"]!!)

    override fun meta(logEvent: FlowLogEvent): Map<String, String> {
        val name: StringField by logEvent.event.fields
        val description: StringField by logEvent.event.fields
        val animationUrl: StringField by logEvent.event.fields
        val hash: StringField by logEvent.event.fields
        val type: StringField by logEvent.event.fields

        return mapOf(
            "name" to name.value!!,
            "description" to description.value!!,
            "animationUrl" to animationUrl.value!!,
            "hash" to hash.value!!,
            "type" to type.value!!
        )

    }
}