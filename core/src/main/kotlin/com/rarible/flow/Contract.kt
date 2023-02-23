package com.rarible.flow

import com.nftco.flow.sdk.AddressRegistry
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Part

interface Contract {
    val contractName: String
    val deployments: Map<FlowChainId, FlowAddress>
    val import: String

    fun supports(itemId: ItemId): Boolean =
        itemId.contract.endsWith(this.contractName)

    fun register(registry: AddressRegistry): AddressRegistry {
        return deployments.entries.fold(registry) { reg, (chain, addr) ->
            reg.register(this.import, addr, chain)
        }
    }

    fun fqn(chain: FlowChainId): String {
        val address = deployments[chain]
        if (address == null) {
            throw IllegalArgumentException("No deployment of contract $contractName exists on chainId $chain")
        } else {
            return "A.${address.base16Value}.$contractName"
        }
    }

    fun staticRoyalties(chain: FlowChainId): List<Part> = emptyList()
}