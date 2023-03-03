package com.rarible.flow.scanner.service

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.Contracts
import com.rarible.flow.scanner.config.FlowListenerProperties
import com.rarible.flow.scanner.model.NonFungibleTokenEventType
import org.springframework.stereotype.Component

@Component
class SupportedNftCollectionProvider(
    private val properties: FlowListenerProperties
) {

    private val nftContracts = Contracts
        .values()
        .filter { it.nft && it.enabled }

    fun get(): Set<String> {
        return get(properties.chainId)
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