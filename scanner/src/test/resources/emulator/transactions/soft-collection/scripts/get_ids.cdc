import NonFungibleToken from 0xNONFUNGIBLETOKEN
import SoftCollection from 0xSOFTCOLLECTION

// Take SoftCollection ids by account address
//
pub fun main(address: Address): [UInt64]? {
    let collection = getAccount(address)
        .getCapability(SoftCollection.CollectionPublicPath)
        .borrow<&{NonFungibleToken.CollectionPublic}>()
        ?? panic("SoftCollection Collection not found")
    return collection.getIDs()
}
