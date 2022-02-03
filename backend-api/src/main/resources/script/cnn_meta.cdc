//import CNN_NFT from 0x329feb3ab062d289
import CNN_NFT from 0xCNNNFT

pub fun main(setId: UInt32): {String: String}? {
    let metaMap = CNN_NFT.getSetMetadata(setId: setId)
    if metaMap == nil {return nil}
    metaMap!.insert(key: "maxEditions", CNN_NFT.getSetMaxEditions(setId: setId)!.toString())
    return metaMap!
}
