import RaribleNFTv2 from 0xRARIBLENFT_V2

pub fun main(address: Address, ids: [UInt64]): [RaribleNFTv2.NFT] {
    let collection = getAccount(address)
            .getCapability(RaribleNFT.collectionPublicPath)
            .borrow<&{NonFungibleToken.CollectionPublic}>()
            ?? panic("RaribleNFT Collection not found")
    let result = []
    for id in ids {
        let item = collection.borrowNFT(id: tokenId)!
        result.appent(item)
    }
    return result
}
