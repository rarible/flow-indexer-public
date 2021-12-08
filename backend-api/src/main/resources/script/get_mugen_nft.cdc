import NonFungibleToken from 0xNONFUNGIBLETOKEN
import MugenNFT from 0xMUGENNFT

// Take MugenNFT token props by account address and tokenId
//
pub fun main(address: Address, tokenId: UInt64): &AnyResource {
    let collection = getAccount(address)
        .getCapability(MugenNFT.CollectionPublicPath)
        .borrow<&{NonFungibleToken.CollectionPublic}>()
        ?? panic("MugenNFT Collection not found")
    return collection.borrowNFT(id: tokenId)
}
