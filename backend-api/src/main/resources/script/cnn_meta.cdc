//import CNN_NFT from 0xCNNNFT

import CNN_NFT from 0x329feb3ab062d289


pub fun main(setId: UInt32, editionNum: UInt32): String? {
    return CNN_NFT.getIpfsMetadataHashByNftEdition(setId: setId, editionNum: editionNum)
}
