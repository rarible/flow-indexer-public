package com.rarible.flow.scanner.listener.activity

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.FlowLogEvent
import com.rarible.flow.core.domain.Part
import com.rarible.flow.scanner.listener.NFTActivityMaker
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class CryptoPiggoActivityMaker(
    @Value("\${blockchain.scanner.flow.chainId}")
    private val chainId: FlowChainId,
): NFTActivityMaker() {
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
