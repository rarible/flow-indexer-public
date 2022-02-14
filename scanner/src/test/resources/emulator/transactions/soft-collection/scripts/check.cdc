import NonFungibleToken from 0xNONFUNGIBLETOKEN
import SoftCollection from 0xSOFTCOLLECTION

// check SoftCollection collection is available on given address
//
pub fun main(address: Address): Bool {
    return getAccount(address)
        .getCapability<&{NonFungibleToken.CollectionPublic}>(SoftCollection.CollectionPublicPath)
        .check()
}
