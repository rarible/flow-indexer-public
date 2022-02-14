package com.rarible.flow

import com.nftco.flow.sdk.AddressRegistry
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.core.domain.ItemId

interface Contract {
    val contractName: String
    val deployments: Map<FlowChainId, FlowAddress>
    val import: String

    fun supports(itemId: ItemId): Boolean =
        itemId.contract.contains(this.contractName)

    fun register(registry: AddressRegistry): AddressRegistry {
        return deployments.entries.fold(registry) { reg, (chain, addr) ->
            reg.register(this.import, addr, chain)
        }
    }

    fun fqn(chain: FlowChainId): String {
        val address = deployments[chain]
        if(address == null) {
            throw IllegalArgumentException("No deployment of contract $contractName exists on chainId $chain")
        } else {
            return "A.${address.base16Value}.$contractName"
        }
    }
}

enum class Contracts: Contract {
    ONE_FOOTBALL {
        override val import: String
            get() = "0xONEFOOTBALL"
        override val contractName: String
            get() = "OneFootballCollectible"
        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                FlowChainId.MAINNET to FlowAddress("0x6831760534292098"),
                FlowChainId.TESTNET to FlowAddress("0x01984fb4ca279d9a"),
                FlowChainId.EMULATOR to FlowAddress("0xf8d6e0586b0a20c7")
            )
    },

    STARLY_CARD {
        override val contractName: String
            get() = "StarlyCard"
        override val import: String
            get() = "0xSTARLY"
        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                FlowChainId.MAINNET to FlowAddress("0x5b82f21c0edf76e3"),
                FlowChainId.EMULATOR to FlowAddress("0xf8d6e0586b0a20c7")
            )
    },

    MATRIX_WORLD_VOUCHER  {
        override val contractName: String
            get() = "MatrixWorldVoucher"
        override val import: String
            get() = "0xMATRIXWORLDVOUCHER"
        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                FlowChainId.MAINNET to FlowAddress("0x0d77ec47bbad8ef6"),
                FlowChainId.TESTNET to FlowAddress("0xebf4ae01d1284af8"),
                FlowChainId.EMULATOR to FlowAddress("0xf8d6e0586b0a20c7")
            )
    },

    MATRIX_WORLD_FLOW_FEST {
        override val contractName: String
            get() = "MatrixWorldFlowFestNFT"
        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                FlowChainId.MAINNET to FlowAddress("0x2d2750f240198f91"),
                FlowChainId.TESTNET to FlowAddress("0xe2f1b000e0203c1d"),
                FlowChainId.EMULATOR to FlowAddress("0xf8d6e0586b0a20c7")
            )
        override val import: String
            get() = "0xMATRIXWORLDFLOWFEST"
    },

    ENGLISH_AUCTION {
        override val contractName: String
            get() = "EnglishAuction"
        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                FlowChainId.MAINNET to FlowAddress("0x01"), //todo fill after deploy to mainnet
                FlowChainId.TESTNET to FlowAddress("0xebf4ae01d1284af8"),
                FlowChainId.EMULATOR to FlowAddress("0xf8d6e0586b0a20c7"),
            )
        override val import: String
            get() = "0xENGLISH_AUCTION"

    }
}
