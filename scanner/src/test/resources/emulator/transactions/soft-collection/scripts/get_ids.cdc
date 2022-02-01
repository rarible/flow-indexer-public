import NonFungibleToken from "../../../../contracts/core/NonFungibleToken.cdc"
import SoftCollection from "../../../../contracts/SoftCollection.cdc"

// Take SoftCollection ids by account address
//
pub fun main(address: Address): [UInt64]? {
    let collection = getAccount(address)
        .getCapability(SoftCollection.CollectionPublicPath)
        .borrow<&{NonFungibleToken.CollectionPublic}>()
        ?? panic("SoftCollection Collection not found")
    return collection.getIDs()
}
