import NonFungibleToken from 0xNONFUNGIBLETOKEN
import RaribleNFT from 0xRARIBLETOKEN

// Take RaribleNFT token props by account address and tokenId
//
pub fun main(address: Address, tokenId: UInt64): &AnyResource {
    let collection = getAccount(address)
        .getCapability<&{NonFungibleToken.CollectionPublic}>(RaribleNFT.collectionPublicPath)
        .borrow()
        ?? panic("NFT Collection not found")
    return collection.borrowNFT(id: tokenId)!
}
