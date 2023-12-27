package com.rarible.flow.scanner.service

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.Contracts
import com.rarible.flow.scanner.model.NonFungibleTokenEventType
import org.springframework.stereotype.Component

@Component
class SupportedNftCollectionProvider(
    private val chainId: FlowChainId
) {

    private val nftContracts = Contracts
        .values()
        .filter { it.nft && it.enabled }

    fun get(): Set<String> {
        return get(chainId)
    }

    fun get(chainId: FlowChainId): Set<String> {
        return nftContracts
            .mapNotNull { contract ->
                if (contract.deployments[chainId] != null) {
                    contract.fqn(chainId)
                } else null
            }
            .toSet()
    }

    fun getEvents(): Set<String> {
        return getEvents(chainId)
    }

    fun getEvents(chainId: FlowChainId): Set<String> {
        return get(chainId)
            .flatMap {
                listOf(
                    NonFungibleTokenEventType.WITHDRAW.full(it),
                    NonFungibleTokenEventType.DEPOSIT.full(it),
                )
            }
            .toSet()
    }
}
