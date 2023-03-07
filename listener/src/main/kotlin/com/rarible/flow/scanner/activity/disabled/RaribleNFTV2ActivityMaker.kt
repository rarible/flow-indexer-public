package com.rarible.flow.scanner.activity.disabled

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.cadence.StructField
import com.rarible.blockchain.scanner.flow.repository.FlowLogRepository
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Part
import com.rarible.flow.core.event.RaribleNFTv2Meta
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.activity.nft.NFTActivityMaker
import com.rarible.flow.scanner.config.FlowListenerProperties

class RaribleNFTV2ActivityMaker(
    flowLogRepository: FlowLogRepository,
    txManager: TxManager,
    properties: FlowListenerProperties,
) : NFTActivityMaker(flowLogRepository, txManager, properties) {

    override val contractName: String = Contracts.RARIBLE_NFTV2.contractName

    override fun tokenId(logEvent: FlowLogEvent): Long {
        return cadenceParser.long(logEvent.event.fields["id"]!!)
    }

    override fun meta(logEvent: FlowLogEvent): Map<String, String> {
        val meta by logEvent.event.fields
        val rariMeta = cadenceParser.unmarshall<RaribleNFTv2Meta>(meta, Contracts.RARIBLE_NFTV2.deployments[chainId]!!)
        return rariMeta.toMap()
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

    override suspend fun itemCollection(mintEvent: FlowLogEvent): String {
        val parentId = cadenceParser.long(mintEvent.event.fields["parentId"]!!)
        return "${ItemId(Contracts.SOFT_COLLECTION.fqn(chainId), parentId)}"
    }
}