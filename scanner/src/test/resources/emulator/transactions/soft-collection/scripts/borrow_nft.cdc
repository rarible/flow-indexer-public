import NonFungibleToken from "../../../../contracts/core/NonFungibleToken.cdc"
import SoftCollection from "../../../../contracts/SoftCollection.cdc"

// Take SoftCollection token props by account address and tokenId
//
pub fun main(address: Address, tokenId: UInt64): &AnyResource {
    let collection = getAccount(address)
        .getCapability(SoftCollection.CollectionPublicPath)
        .borrow<&{NonFungibleToken.CollectionPublic}>()
        ?? panic("SoftCollection Collection not found")
    return collection.borrowNFT(id: tokenId)
}
