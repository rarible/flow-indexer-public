import NonFungibleToken from 0xNONFUNGIBLETOKEN
import Art from 0xVERSUSART

// Take Art token props by account address and tokenId
//
pub fun main(address: Address, tokenId: UInt64): &AnyResource {
    let collection = getAccount(address)
        .getCapability(Art.CollectionPublicPath)
        .borrow<&{Art.CollectionPublic}>()
        ?? panic("Art Collection not found")
    return collection.borrowNFT(id: tokenId)
}
