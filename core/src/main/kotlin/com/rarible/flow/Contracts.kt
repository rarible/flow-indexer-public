package com.rarible.flow

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.RoyaltySize.FIVE_PERCENT
import com.rarible.flow.RoyaltySize.TEN_PERCENT
import com.rarible.flow.RoyaltySize.percent
import com.rarible.flow.core.domain.Part

enum class Contracts : Contract {
    HW_GARAGE_CARD {

        override val contractName: String = "HWGarageCard"

        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                //https://flowscan.org/contract/A.d0bcefdf1e67ea85.HWGarageCard
                FlowChainId.MAINNET to FlowAddress("0xd0bcefdf1e67ea85"),
                //https://testnet.flowscan.org/contract/A.6f6702697b205c18.HWGarageCard
                FlowChainId.TESTNET to FlowAddress("0x6f6702697b205c18"),
                FlowChainId.EMULATOR to FlowAddress("0x9f36754d9b38f155"),
            )

        override val import: String
            get() = "0xHWGARAGECARD"

        override val symbol: String
            get() = "HWGC"
    },

    HW_GARAGE_PACK {

        override val contractName: String = "HWGaragePack"

        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                //https://flowscan.org/contract/A.d0bcefdf1e67ea85.HWGaragePack
                FlowChainId.MAINNET to FlowAddress("0xd0bcefdf1e67ea85"),
                //https://testnet.flowscan.org/contract/A.6f6702697b205c18.HWGaragePack
                FlowChainId.TESTNET to FlowAddress("0x6f6702697b205c18"),
                FlowChainId.EMULATOR to FlowAddress("0x9f36754d9b38f155"),
            )

        override val import: String
            get() = "0xHWGARAGEPACK"

        override val symbol: String
            get() = "HWGP"
    },

    HW_GARAGE_CARD_V2 {

        override val contractName: String = "HWGarageCardV2"

        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                //https://flowscan.org/contract/A.d0bcefdf1e67ea85.HWGarageCardV2
                FlowChainId.MAINNET to FlowAddress("0xd0bcefdf1e67ea85"),
                //https://testnet.flowscan.org/contract/A.6f6702697b205c18.HWGarageCardV2
                FlowChainId.TESTNET to FlowAddress("0x6f6702697b205c18"),
                FlowChainId.EMULATOR to FlowAddress("0xf8d6e0586b0a20c7"),
            )

        override val import: String
            get() = "0xHWGARAGECARDV2"

        override val symbol: String
            get() = "HWGCV2"
    },

    HW_GARAGE_PACK_V2 {

        override val contractName: String = "HWGaragePackV2"

        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                //https://flowscan.org/contract/A.d0bcefdf1e67ea85.HWGaragePackV2
                FlowChainId.MAINNET to FlowAddress("0xd0bcefdf1e67ea85"),
                //https://testnet.flowscan.org/contract/A.6f6702697b205c18.HWGaragePackV2
                FlowChainId.TESTNET to FlowAddress("0x6f6702697b205c18"),
                FlowChainId.EMULATOR to FlowAddress("0x9f36754d9b38f155"),
            )

        override val import: String
            get() = "0xHWGARAGEPACKV2"

        override val symbol: String
            get() = "HWGPV2"
    },

    HW_GARAGE_PM {

        override val contractName: String = "HWGaragePM"

        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                //https://flowscan.org/contract/A.d0bcefdf1e67ea85.HWGaragePM
                FlowChainId.MAINNET to FlowAddress("0xd0bcefdf1e67ea85"),
                //https://testnet.flowscan.org/contract/A.6f6702697b205c18.HWGaragePM
                FlowChainId.TESTNET to FlowAddress("0x6f6702697b205c18"),
                FlowChainId.EMULATOR to FlowAddress("0x9f36754d9b38f155"),
            )

        override val import: String
            get() = "0xHWGARAGEPM"

        override val nft: Boolean
            get() = false
    },

    HW_GARAGE_PM_V2 {

        override val contractName: String = "HWGaragePMV2"

        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                //https://flowscan.org/contract/A.d0bcefdf1e67ea85.HWGaragePMV2
                FlowChainId.MAINNET to FlowAddress("0xd0bcefdf1e67ea85"),
                //https://testnet.flowscan.org/contract/A.6f6702697b205c18.HWGaragePMV2
                FlowChainId.TESTNET to FlowAddress("0x6f6702697b205c18"),
                FlowChainId.EMULATOR to FlowAddress("0x9f36754d9b38f155"),
            )

        override val import: String
            get() = "0xHWGARAGEPMV2"

        override val nft: Boolean
            get() = false
    },

    BARBIE_CARD {

        override val contractName: String = "BBxBarbieCard"
        override val import = "0xBBXBARBIECARD"
        override val symbol = "BBBC"

        override val deployments = mapOf(
            //https://flowscan.org/contract/A.e5bf4d436ca23932.BBxBarbieCard
            FlowChainId.MAINNET to FlowAddress("0xe5bf4d436ca23932"),
            //https://testnet.flowscan.org/contract/A.6d0f55821f6b2dbe.BBxBarbieCard
            FlowChainId.TESTNET to FlowAddress("0x6d0f55821f6b2dbe"),
            FlowChainId.EMULATOR to FlowAddress("0xeff462cf475a0c02"),
        )
    },

    BARBIE_PACK {

        override val contractName: String = "BBxBarbiePack"
        override val import = "0xBBXBARBIEPACK"
        override val symbol = "BBBP"

        override val deployments = mapOf(
            //https://flowscan.org/contract/A.e5bf4d436ca23932.BBxBarbiePack
            FlowChainId.MAINNET to FlowAddress("0xe5bf4d436ca23932"),
            //https://testnet.flowscan.org/contract/A.6d0f55821f6b2dbe.BBxBarbiePack
            FlowChainId.TESTNET to FlowAddress("0x6d0f55821f6b2dbe"),
            FlowChainId.EMULATOR to FlowAddress("0xeff462cf475a0c02"),
        )
    },

    BARBIE_TOKEN {

        override val contractName: String = "BBxBarbieToken"
        override val import = "0xBBXBARBIETOKEN"
        override val symbol = "BBBT"

        override val deployments = mapOf(
            //https://flowscan.org/contract/A.e5bf4d436ca23932.BBxBarbieToken
            FlowChainId.MAINNET to FlowAddress("0xe5bf4d436ca23932"),
            //https://testnet.flowscan.org/contract/A.6d0f55821f6b2dbe.BBxBarbieToken
            FlowChainId.TESTNET to FlowAddress("0x6d0f55821f6b2dbe"),
            FlowChainId.EMULATOR to FlowAddress("0xeff462cf475a0c02"),
        )
    },

    BARBIE_PM {

        override val contractName: String = "BBxBarbiePM"
        override val import = "0xBBXBARBIEPM"
        override val nft = false

        override val deployments = mapOf(
            //https://flowscan.org/contract/A.e5bf4d436ca23932.BBxBarbiePM
            FlowChainId.MAINNET to FlowAddress("0xe5bf4d436ca23932"),
            //https://testnet.flowscan.org/contract/A.6d0f55821f6b2dbe.BBxBarbiePM
            FlowChainId.TESTNET to FlowAddress("0x6d0f55821f6b2dbe"),
            FlowChainId.EMULATOR to FlowAddress("0xeff462cf475a0c02"),
        )
    },

    RARIBLE_NFT {

        override val contractName: String = "RaribleNFT"

        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                //https://flowscan.org/contract/A.01ab36aaf654a13e.RaribleNFT
                FlowChainId.MAINNET to FlowAddress("0x01ab36aaf654a13e"),
                //https://flowscan.org/contract/A.ebf4ae01d1284af8.RaribleNFT
                FlowChainId.TESTNET to FlowAddress("0xebf4ae01d1284af8"),
                FlowChainId.EMULATOR to FlowAddress("0xf8d6e0586b0a20c7"),
            )

        override val import: String
            get() = "0xRARIBLENFT"

        override val collectionName: String
            get() = "Rarible"

        override val symbol: String
            get() = "RARIBLE"

        override val features: Set<String>
            get() = setOf("SECONDARY_SALE_FEES", "BURN")
    },

    RARIBLE_GARAGE_CARD {

        override val contractName: String = "HWGarageCard"

        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                //https://testnet.flowscan.org/contract/A.80102bce1de42dc4.HWGarageCard
                FlowChainId.TESTNET to FlowAddress("0x80102bce1de42dc4"),
                FlowChainId.EMULATOR to FlowAddress("0x80102bce1de42dc4"),
            )

        override val import: String
            get() = "0xHWGARAGECARD"

        override val symbol: String
            get() = "HWGC"
    },

    RARIBLE_GARAGE_PACK {

        override val contractName: String = "HWGaragePack"

        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                //https://testnet.flowscan.org/contract/A.80102bce1de42dc4.HWGaragePack
                FlowChainId.TESTNET to FlowAddress("0x80102bce1de42dc4"),
                FlowChainId.EMULATOR to FlowAddress("0x80102bce1de42dc4"),
            )

        override val import: String
            get() = "0xHWGARAGEPACK"

        override val symbol: String
            get() = "HWGP"
    },

    RARIBLE_GARAGE_CARD_V2 {

        override val contractName: String = "HWGarageCardV2"

        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                //https://testnet.flowscan.org/contract/A.80102bce1de42dc4.HWGarageCardV2
                FlowChainId.TESTNET to FlowAddress("0x80102bce1de42dc4"),
                FlowChainId.EMULATOR to FlowAddress("0x80102bce1de42dc4"),
            )

        override val import: String
            get() = "0xHWGARAGECARDV2"

        override val symbol: String
            get() = "HWGCV2"
    },

    RARIBLE_GARAGE_PACK_V2 {

        override val contractName: String = "HWGaragePackV2"

        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                //https://testnet.flowscan.org/contract/A.80102bce1de42dc4.HWGaragePackV2
                FlowChainId.TESTNET to FlowAddress("0x80102bce1de42dc4"),
                FlowChainId.EMULATOR to FlowAddress("0x80102bce1de42dc4"),
            )

        override val import: String
            get() = "0xHWGARAGEPACKV2"

        override val symbol: String
            get() = "HWGPV2"
    },

    RARIBLE_GARAGE_PM {

        override val contractName: String = "HWGaragePM"

        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                //https://testnet.flowscan.org/contract/A.80102bce1de42dc4.HWGaragePM
                FlowChainId.TESTNET to FlowAddress("0x80102bce1de42dc4"),
                FlowChainId.MAINNET to FlowAddress("0x80102bce1de42dc4"),
                FlowChainId.EMULATOR to FlowAddress("0x80102bce1de42dc4"),
            )

        override val import: String
            get() = "0xHWGARAGEPM"

        override val nft: Boolean
            get() = false
    },

    RARIBLE_GARAGE_PM_V2 {

        override val contractName: String = "HWGaragePMV2"

        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                //https://testnet.flowscan.org/contract/A.80102bce1de42dc4.HWGaragePMV2
                FlowChainId.TESTNET to FlowAddress("0x80102bce1de42dc4"),
                FlowChainId.EMULATOR to FlowAddress("0x80102bce1de42dc4"),
            )

        override val import: String
            get() = "0xHWGARAGEPMV2"

        override val nft: Boolean
            get() = false
    },

    RARIBLE_BARBIE_CARD {

        override val contractName: String = "BBxBarbieCard"
        override val import = "0XBBXBARBIECARD"
        override val symbol = "BBBC"

        override val deployments = mapOf(
            FlowChainId.TESTNET to FlowAddress("0x80102bce1de42dc4"),
        )
    },

    RARIBLE_BARBIE_PACK {

        override val contractName: String = "BBxBarbiePack"
        override val import = "0xBBXBARBIEPACK"
        override val symbol = "BBBP"

        override val deployments = mapOf(
            FlowChainId.TESTNET to FlowAddress("0x80102bce1de42dc4"),
        )
    },

    RARIBLE_BARBIE_TOKEN {

        override val contractName: String = "BBxBarbieToken"
        override val import = "0xBBXBARBIETOKEN"
        override val symbol = "BBBT"

        override val deployments = mapOf(
            FlowChainId.TESTNET to FlowAddress("0x80102bce1de42dc4"),
        )
    },

    RARIBLE_BARBIE_PM {

        override val contractName: String = "BBxBarbiePM"
        override val import = "0xBBXBARBIEPM"
        override val nft = false

        override val deployments = mapOf(
            FlowChainId.TESTNET to FlowAddress("0x80102bce1de42dc4"),
        )
    },

    NFT_STOREFRONT {

        override val contractName = "NFTStorefront"

        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                //https://flowscan.org/contract/A.4eb8a10cb9f87357.NFTStorefront
                FlowChainId.MAINNET to FlowAddress("0x4eb8a10cb9f87357"),
                FlowChainId.TESTNET to FlowAddress("0x94b06cfca1d8a476"),
                FlowChainId.EMULATOR to FlowAddress("0xf8d6e0586b0a20c7"),
            )
        override val import: String
            get() = "0x${contractName.uppercase()}"

        override val nft: Boolean
            get() = false
    },

    NFT_STOREFRONT_V2 {

        override val contractName = "NFTStorefrontV2"

        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                //https://flowscan.org/contract/A.4eb8a10cb9f87357.NFTStorefrontV2
                FlowChainId.MAINNET to FlowAddress("0x4eb8a10cb9f87357"),
                //https://testnet.flowscan.org/contract/A.80102bce1de42dc4.NFTStorefrontV2
                FlowChainId.TESTNET to FlowAddress("0x80102bce1de42dc4"),
            )
        override val import: String
            get() = "0x${contractName.uppercase()}"

        override val nft: Boolean
            get() = false
    },

    METADATA_VIEWS {

        override val contractName = "MetadataViews"

        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                //https://flowscan.org/contract/A.1d7e57aa55817448.MetadataViews
                FlowChainId.MAINNET to FlowAddress("0x1d7e57aa55817448"),
                //https://testnet.flowscan.org/contract/A.631e88ae7f1d7c20.MetadataViews
                FlowChainId.TESTNET to FlowAddress("0x631e88ae7f1d7c20"),
            )
        override val import: String
            get() = "0x${contractName.uppercase()}"

        override val nft: Boolean
            get() = false
    },

    //=========== Disabled contracts
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

        override fun staticRoyalties(chain: FlowChainId): List<Part> {
            return if (chain == FlowChainId.MAINNET) {
                listOf(
                    Part(
                        FlowAddress("0x6831760534292098"),
                        0.5.percent() // 0.5 %
                    )
                )
            } else super.staticRoyalties(chain)
        }

        override val enabled: Boolean
            get() = false

        override val collectionName: String
            get() = "OneFootball"

        override val features: Set<String>
            get() = setOf("BURN")
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
                listOf(Part(FlowAddress("0x12c122ca9266c278"), TEN_PERCENT))
            } else super.staticRoyalties(chain)
        }

        override val enabled: Boolean
            get() = false

        override val collectionName: String
            get() = "Starly"

        override val features: Set<String>
            get() = setOf("BURN")
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
                Part(FlowAddress("0x46f1e88b54fcb73c"), FIVE_PERCENT) // 5%
            ) else super.staticRoyalties(chain)
        }

        override val enabled: Boolean
            get() = false

        override val collectionName: String
            get() = "Matrix World Voucher"

        override val symbol: String
            get() = "MXWRLDV"
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
            return if (chain == FlowChainId.MAINNET) listOf(
                Part(FlowAddress("0x46f1e88b54fcb73c"), FIVE_PERCENT) // 5%
            ) else super.staticRoyalties(chain)
        }

        override val enabled: Boolean
            get() = false

        override val collectionName: String
            get() = "Matrix World Flow Fest"

        override val symbol: String
            get() = "MXWRLDFFEST"
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
                Part(FlowAddress("0x609a2ea0548b4b51"), FIVE_PERCENT) // 5%
            ) else super.staticRoyalties(chain)
        }

        override val enabled: Boolean
            get() = false

        override val collectionName: String
            get() = "Jambb"

        override val features: Set<String>
            get() = setOf("BURN")
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
                Part(FlowAddress("0x55c8be371f74168f"), TEN_PERCENT) // 10%
            ) else super.staticRoyalties(chain)
        }

        override val enabled: Boolean
            get() = false

        override val collectionName: String
            get() = "CNN"

        override val features: Set<String>
            get() = setOf("BURN")
    },

    VERSUS_ART {

        override val contractName = "Art"

        override val deployments = mapOf(
            FlowChainId.MAINNET to FlowAddress("0xd796ff17107bbff6"),
            FlowChainId.TESTNET to FlowAddress("0x99ca04281098b33d"),
            FlowChainId.EMULATOR to FlowAddress("0xf8d6e0586b0a20c7")
        )

        override val import = "0xART"

        override val enabled: Boolean
            get() = false

        override val collectionName: String
            get() = "VersusArt"

        override val symbol: String
            get() = "VERSUS"
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
                Part(FlowAddress("0x77b78d7d3f0d1787"), TEN_PERCENT) // 10%
            ) else super.staticRoyalties(chain)
        }

        override val features: Set<String>
            get() = setOf("BURN")

        override val enabled: Boolean
            get() = false
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

        override val symbol: String
            get() = "MotoGP™"

        override val collectionName: String
            get() = "MotoGP™ Ignition"

        override val features: Set<String>
            get() = setOf("BURN")

        override val enabled: Boolean
            get() = false
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

        override val import = "0x${contractName.uppercase()}"

        override fun staticRoyalties(chain: FlowChainId): List<Part> {
            return if (chain == FlowChainId.MAINNET) {
                listOf(Part(FlowAddress("0xa161c109f0902908"), 15.0.percent()))
            } else super.staticRoyalties(chain)
        }

        override val enabled: Boolean
            get() = false

        override val collectionName: String
            get() = "Fanfare"
    },

    CHAINMONSTERS {

        override val contractName: String
            get() = "ChainmonstersRewards"

        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                FlowChainId.MAINNET to FlowAddress("0x93615d25d14fa337"),
                FlowChainId.TESTNET to FlowAddress("0x75783e3c937304a8"),
                FlowChainId.EMULATOR to FlowAddress("0xf8d6e0586b0a20c7")
            )

        override val import: String
            get() = "0xCHAINMONSTERS"

        override fun staticRoyalties(chain: FlowChainId): List<Part> {
            return if (chain == FlowChainId.MAINNET) {
                listOf(Part(FlowAddress("0x64f83c60989ce555"), FIVE_PERCENT))
            } else super.staticRoyalties(chain)
        }

        override val enabled: Boolean
            get() = false

        override val collectionName: String
            get() = "Chainmonsters"
    },

    BARTER_YARD_PACK {

        override val contractName: String = "BarterYardPackNFT"

        override val deployments: Map<FlowChainId, FlowAddress> = mapOf(
            FlowChainId.MAINNET to FlowAddress("0xa95b021cf8a30d80"),
            FlowChainId.TESTNET to FlowAddress("0x4300fc3a11778a9a"),
        )

        override val import: String = "0xBARTERYARDPACKNFT"

        override fun staticRoyalties(chain: FlowChainId): List<Part> = when (chain) {
            FlowChainId.MAINNET -> listOf(Part(FlowAddress("0xb07b788eb60b6528"), FIVE_PERCENT))
            FlowChainId.TESTNET -> listOf(Part(FlowAddress("0x4300fc3a11778a9a"), FIVE_PERCENT))
            else -> super.staticRoyalties(chain)
        }

        override val enabled: Boolean
            get() = false

        override val collectionName: String
            get() = "Barter Yard Club - Mint Pass"

        override val symbol: String
            get() = "BYC-MP"

        override val features: Set<String>
            get() = setOf("BURN")
    },

    KICKS {

        override val contractName: String
            get() = "Kicks"

        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                FlowChainId.MAINNET to FlowAddress("0xf3cc54f4d91c2f6c"),
                FlowChainId.TESTNET to FlowAddress("0xe861e151d3556d70"),
                FlowChainId.EMULATOR to FlowAddress("0xf8d6e0586b0a20c7")
            )

        override val import: String
            get() = "0xKICKS"

        override fun staticRoyalties(chain: FlowChainId): List<Part> {
            return if (chain == FlowChainId.MAINNET) {
                listOf(Part(FlowAddress("0xf3cc54f4d91c2f6c"), FIVE_PERCENT))
            } else super.staticRoyalties(chain)
        }

        override val enabled: Boolean
            get() = false

        override val features: Set<String>
            get() = setOf("BURN")
    },

    DISRUPT_ART {

        override val contractName: String
            get() = "DisruptArt"

        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                FlowChainId.MAINNET to FlowAddress("0xcd946ef9b13804c6"),
                FlowChainId.TESTNET to FlowAddress("0x439c2b49c0b2f62b"),
                FlowChainId.EMULATOR to FlowAddress("0xf8d6e0586b0a20c7"),
            )

        override val import: String
            get() = "0xDISRUPTART"

        override fun staticRoyalties(chain: FlowChainId): List<Part> {
            return listOfNotNull(
                when (chain) {
                    FlowChainId.MAINNET -> Part(FlowAddress("0x420f47f16a214100"), 15.0.percent())
                    FlowChainId.TESTNET -> Part(FlowAddress("0x439c2b49c0b2f62b"), 15.0.percent())
                    else -> null
                }
            )
        }

        override val enabled: Boolean
            get() = false

        override val symbol: String
            get() = "DISRUPT ART"
    },

    TOPSHOT {

        override val contractName: String
            get() = "TopShot"
        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                FlowChainId.MAINNET to FlowAddress("0x0b2a3299cc857e29"),
                FlowChainId.TESTNET to FlowAddress("0x01658d9b94068f3c"),
                FlowChainId.EMULATOR to FlowAddress("0xf8d6e0586b0a20c7"),
            )
        override val import: String
            get() = "0xTOPSHOT"

        override fun staticRoyalties(chain: FlowChainId): List<Part> {
            return if (chain == FlowChainId.MAINNET) {
                listOf(
                    Part(
                        address = FlowAddress("0x0b2a3299cc857e29"),
                        fee = 0.05
                    )
                )
            } else super.staticRoyalties(chain)
        }

        override val symbol: String
            get() = "NBA TS"

        override val collectionName: String
            get() = "NBA Top Shot"

        override val features: Set<String>
            get() = setOf("BURN")

        override val enabled: Boolean
            get() = false
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
            get() = "0xENGLISHAUCTION"

        override val enabled: Boolean
            get() = false
    },

    SOFT_COLLECTION {

        override val contractName: String = "SoftCollection"

        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                FlowChainId.MAINNET to FlowAddress("0x01ab36aaf654a13e"),
                FlowChainId.TESTNET to FlowAddress("0xebf4ae01d1284af8"),
                FlowChainId.EMULATOR to FlowAddress("0xf8d6e0586b0a20c7"),
            )

        override val import: String = "0xSOFTCOLLECTION"

        override val enabled: Boolean
            get() = false
    },

    RARIBLE_NFTV2 {

        override val contractName: String = "RaribleNFTv2"

        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                FlowChainId.MAINNET to FlowAddress("0x01ab36aaf654a13e"),
                FlowChainId.TESTNET to FlowAddress("0xebf4ae01d1284af8"),
                FlowChainId.EMULATOR to FlowAddress("0xf8d6e0586b0a20c7"),
            )

        override val import: String
            get() = "0xRARIBLENFTV2"

        override val enabled: Boolean
            get() = false

        override val collectionName: String
            get() = "RaribleV2"

        override val symbol: String
            get() = "RARIBLE_V2"

        override val features: Set<String>
            get() = setOf("SECONDARY_SALE_FEES", "BURN")
    },

    SOME_PLACE_COLLECTIBLE {

        override val contractName: String
            get() = "SomePlaceCollectible"

        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                FlowChainId.MAINNET to FlowAddress("0x667a16294a089ef8"),
                FlowChainId.TESTNET to FlowAddress("0x0c153e28da9f988a"),
                FlowChainId.EMULATOR to FlowAddress("0xf8d6e0586b0a20c7"),
            )

        override val import: String
            get() = "0x${contractName.uppercase()}"

        override fun staticRoyalties(chain: FlowChainId): List<Part> = when (chain) {
            FlowChainId.MAINNET -> listOf(Part(FlowAddress("0x8e2e0ebf3c03aa88"), TEN_PERCENT))
            else -> super.staticRoyalties(chain)
        }

        override val enabled: Boolean
            get() = false

        override val collectionName: String
            get() = "The Potion"

        override val symbol: String
            get() = "POTION"

        override val features: Set<String>
            get() = setOf("BURN")
    },

    GENIACE {

        override val contractName = "GeniaceNFT"

        override val deployments = mapOf(
            FlowChainId.MAINNET to FlowAddress("0xabda6627c70c7f52"),
            FlowChainId.TESTNET to FlowAddress("0x99eb28310626e56a"),
            FlowChainId.EMULATOR to FlowAddress("0xf8d6e0586b0a20c7"),
        )

        override val import = "0x${contractName.uppercase()}"

        override fun staticRoyalties(chain: FlowChainId): List<Part> {
            return when (chain) {
                FlowChainId.MAINNET -> listOf(Part(FlowAddress("0x0bfbaa1760ead010"), TEN_PERCENT))
                else -> super.staticRoyalties(chain)
            }
        }

        override val enabled: Boolean
            get() = false

        override val collectionName: String
            get() = "Geniace"

        override val symbol: String
            get() = "GEN"
    },

    CRYPTOPIGGO {

        override val contractName: String
            get() = "CryptoPiggo"
        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                FlowChainId.MAINNET to FlowAddress("0xd3df824bf81910a4"),
                FlowChainId.TESTNET to FlowAddress("0x57e1b27618c5bb69"),
                FlowChainId.EMULATOR to FlowAddress("0xf8d6e0586b0a20c7"),
            )

        override val import: String
            get() = "0x${contractName.uppercase()}"

        override fun staticRoyalties(chain: FlowChainId): List<Part> {
            return when (chain) {
                FlowChainId.MAINNET -> listOf(Part(FlowAddress("0x4bf7023c25942322"), 3.5.percent()))
                else -> super.staticRoyalties(chain)
            }
        }

        override val enabled: Boolean
            get() = false

        override val symbol: String
            get() = "CPIG"
    },

    MUGEN {

        override val contractName: String
            get() = "MugenNFT"
        override val deployments: Map<FlowChainId, FlowAddress>
            get() = mapOf(
                FlowChainId.MAINNET to FlowAddress("0x2cd46d41da4ce262"),
                FlowChainId.TESTNET to FlowAddress("0xebf4ae01d1284af8"),
                FlowChainId.EMULATOR to FlowAddress("0xf8d6e0586b0a20c7"),
            )

        override val import: String
            get() = "0x${contractName.uppercase()}"

        override val enabled: Boolean
            get() = false

        override val collectionName: String
            get() = "Mugen"
    }
}

