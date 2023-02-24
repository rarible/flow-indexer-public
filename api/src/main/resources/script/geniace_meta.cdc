import NonFungibleToken from 0xNONFUNGIBLETOKEN
import GeniaceNFT from 0xGENIACENFT

// Take GeniaceNFT token props by account address and tokenId
//
pub fun main(address: Address, tokenId: UInt64): &AnyResource {
    let collection = getAccount(address)
        .getCapability(GeniaceNFT.CollectionPublicPath)
        .borrow<&{GeniaceNFT.GeniaceNFTCollectionPublic}>()
        ?? panic("GeniaceNFT Collection not found")
    return collection.borrowNFT(id: tokenId)
}
