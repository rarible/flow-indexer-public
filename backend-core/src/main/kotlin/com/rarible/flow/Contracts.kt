package com.rarible.flow

import com.nftco.flow.sdk.AddressRegistry
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.RoyaltySize.percent
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.Part

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
        if (address == null) {
            throw IllegalArgumentException("No deployment of contract $contractName exists on chainId $chain")
        } else {
            return "A.${address.base16Value}.$contractName"
        }
    }

    fun staticRoyalties(chain: FlowChainId): List<Part> = emptyList()
}

enum class Contracts : Contract {
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

        override fun staticRoyalties(chain: FlowChainId): List<Part> {
            return if (chain == FlowChainId.MAINNET) {
                listOf(Part(FlowAddress("0x12c122ca9266c278"), RoyaltySize.TEN_PERCENT))
            } else super.staticRoyalties(chain)
        }
    },

    MATRIX_WORLD_VOUCHER {
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

        override fun staticRoyalties(chain: FlowChainId): List<Part> {
            return if (chain == FlowChainId.MAINNET) listOf(
                Part(FlowAddress("0x46f1e88b54fcb73c"), RoyaltySize.FIVE_PERCENT) // 5%
            ) else super.staticRoyalties(chain)
        }
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

        override fun staticRoyalties(chain: FlowChainId): List<Part> {
            return if(chain == FlowChainId.MAINNET) listOf(
                Part(FlowAddress("0x46f1e88b54fcb73c"), RoyaltySize.FIVE_PERCENT) // 5%
            ) else super.staticRoyalties(chain)
        }
    },

    JAMBB_MOMENTS {
        override val contractName: String
            get() = "Moments"
        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                FlowChainId.MAINNET to FlowAddress("0xd4ad4740ee426334"),
                FlowChainId.TESTNET to FlowAddress("0xe94a6e229293f196"),
                FlowChainId.EMULATOR to FlowAddress("0xf8d6e0586b0a20c7")
            )
        override val import: String
            get() = "0xJAMBBMOMENTS"

        override fun staticRoyalties(chain: FlowChainId): List<Part> {
            return if (chain == FlowChainId.MAINNET) listOf(
                Part(FlowAddress("0x609a2ea0548b4b51"), RoyaltySize.FIVE_PERCENT) // 5%
            ) else super.staticRoyalties(chain)
        }
    },

    CNN {
        override val contractName: String
            get() = "CNN_NFT"
        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                FlowChainId.MAINNET to FlowAddress("0x329feb3ab062d289"),
                FlowChainId.TESTNET to FlowAddress("0xebf4ae01d1284af8"),
                FlowChainId.EMULATOR to FlowAddress("0xf8d6e0586b0a20c7")
            )
        override val import: String
            get() = "0xCNNNFT"

        override fun staticRoyalties(chain: FlowChainId): List<Part> {
            return if (chain == FlowChainId.MAINNET) listOf(
                Part(FlowAddress("0x55c8be371f74168f"), RoyaltySize.TEN_PERCENT) // 10%
            ) else super.staticRoyalties(chain)
        }
    },

    VERSUS_ART {
        override val contractName: String
            get() = "Art"
        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                FlowChainId.MAINNET to FlowAddress("0xd796ff17107bbff6"),
                FlowChainId.TESTNET to FlowAddress("0x99ca04281098b33d"),
                FlowChainId.EMULATOR to FlowAddress("0xf8d6e0586b0a20c7")
            )
        override val import: String
            get() = "0xVERSUSART"
    },

    EVOLUTION {
        override val contractName: String
            get() = "Evolution"
        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                FlowChainId.MAINNET to FlowAddress("0xf4264ac8f3256818"),
                FlowChainId.TESTNET to FlowAddress("0x01658d9b94068f3c"),
                FlowChainId.EMULATOR to FlowAddress("0xf8d6e0586b0a20c7")
            )
        override val import: String
            get() = "0xEVOLUTIONTOKEN"

        override fun staticRoyalties(chain: FlowChainId): List<Part> {
            return if (chain == FlowChainId.MAINNET) listOf(
                Part(FlowAddress("0x77b78d7d3f0d1787"), RoyaltySize.TEN_PERCENT) // 10%
            ) else super.staticRoyalties(chain)
        }
    },

    MOTOGP {
        override val contractName: String
            get() = "MotoGPCard"
        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                FlowChainId.MAINNET to FlowAddress("0xa49cc0ee46c54bfb"),
                FlowChainId.TESTNET to FlowAddress("0x01658d9b94068f3c"),
                FlowChainId.EMULATOR to FlowAddress("0xf8d6e0586b0a20c7")
            )
        override val import: String
            get() = "0xMOTOGPTOKEN"

        override fun staticRoyalties(chain: FlowChainId): List<Part> {
            return if (chain == FlowChainId.MAINNET) listOf(
                Part(FlowAddress("0x1b0d0e046c306e2f"), 7.5.percent()) // 7.5%
            ) else super.staticRoyalties(chain)
        }
    },

    FANFARE {
        override val contractName: String
            get() = "FanfareNFTContract"
        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                FlowChainId.MAINNET to FlowAddress("0x4c44f3b1e4e70b20"),
                FlowChainId.TESTNET to FlowAddress("0xd7a4dcfb23d327da"),
                FlowChainId.EMULATOR to FlowAddress("0xf8d6e0586b0a20c7")
            )
        override val import: String
            get() = "0xFANFARE"

        override fun staticRoyalties(chain: FlowChainId): List<Part> {
            return if (chain == FlowChainId.MAINNET) {
                listOf(Part(FlowAddress("0xa161c109f0902908"), 15.0.percent()))
            } else super.staticRoyalties(chain)
        }
    }
}

object RoyaltySize {
    const val TEN_PERCENT = 0.1
    const val FIVE_PERCENT = 0.05

    fun Double.percent() = this.div(100)
}