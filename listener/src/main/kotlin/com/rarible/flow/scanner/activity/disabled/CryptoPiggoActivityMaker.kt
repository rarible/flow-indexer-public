package com.rarible.flow.scanner.activity.disabled

import com.rarible.blockchain.scanner.flow.repository.FlowLogRepository
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.activity.nft.NFTActivityMaker
import com.rarible.flow.scanner.config.FlowListenerProperties

class CryptoPiggoActivityMaker(
    flowLogRepository: FlowLogRepository,
    txManager: TxManager,
    properties: FlowListenerProperties,
) : NFTActivityMaker(flowLogRepository, txManager, properties) {

    override val contractName: String
        get() = Contracts.CRYPTOPIGGO.contractName

    override fun isSupportedCollection(collection: String): Boolean =
        collection == Contracts.CRYPTOPIGGO.fqn(chainId)

    override fun tokenId(logEvent: FlowLogEvent): Long = parse {
        long(logEvent.event.fields["id"]!!)
    }

    override fun meta(logEvent: FlowLogEvent): Map<String, String> = parse {
        dictionaryMap(logEvent.event.fields["initMeta"]!!) { k, v ->
            string(k) to string(v)
        }
    }

    override fun royalties(logEvent: FlowLogEvent): List<Part> = Contracts.CRYPTOPIGGO.staticRoyalties(chainId)
}
