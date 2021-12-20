import CNN_NFT from 0xCNNNFT

pub fun main(setId: UInt32, editionNum: UInt32): String? {
    return CNN_NFT.getIpfsMetadataHashByNftEdition(setId: setId, editionNum: editionNum)
}
