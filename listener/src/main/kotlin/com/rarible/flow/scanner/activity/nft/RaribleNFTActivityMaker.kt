package com.rarible.flow.scanner.activity.nft

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.StructField
import com.rarible.blockchain.scanner.flow.repository.FlowLogRepository
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.TxManager
import org.springframework.stereotype.Component

@Component
class RaribleNFTActivityMaker(
    logRepository: FlowLogRepository,
    txManager: TxManager,
    chainId: FlowChainId,
) : NFTActivityMaker(logRepository, txManager, chainId) {

    override val contractName: String = Contracts.RARIBLE_NFT.contractName

    override fun tokenId(logEvent: FlowLogEvent): Long = cadenceParser.long(logEvent.event.fields["id"]!!)

    override fun meta(logEvent: FlowLogEvent): Map<String, String> {
        return try {
            cadenceParser.dictionaryMap(logEvent.event.fields["metadata"]!!) { key, value ->
                string(key) to string(value)
            }
        } catch (_: Exception) {
            mapOf("metaURI" to cadenceParser.string(logEvent.event.fields["metadata"]!!))
        }
    }

    override fun royalties(logEvent: FlowLogEvent): List<Part> {
        return cadenceParser.arrayValues(logEvent.event.fields["royalties"]!!) {
            it as StructField
            Part(
                address = FlowAddress(address(it.value!!.getRequiredField("address"))),
                fee = double(it.value!!.getRequiredField("fee"))
            )
        }
    }

    override fun creator(logEvent: FlowLogEvent): String {
        return cadenceParser.address(logEvent.event.fields["creator"]!!)
    }
}
