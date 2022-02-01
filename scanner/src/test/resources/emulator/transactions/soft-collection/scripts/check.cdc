import NonFungibleToken from "../../../../contracts/core/NonFungibleToken.cdc"
import SoftCollection from "../../../../contracts/SoftCollection.cdc"

// check SoftCollection collection is available on given address
//
pub fun main(address: Address): Bool {
    return getAccount(address)
        .getCapability<&{NonFungibleToken.CollectionPublic}>(SoftCollection.CollectionPublicPath)
        .check()
}
