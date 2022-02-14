import NonFungibleToken from 0xNONFUNGIBLETOKEN
import SoftCollection from 0xSOFTCOLLECTION

// Setup storage for SoftCollection on signer account
//
transaction {
    prepare(account: AuthAccount) {
        if !account.getCapability<&{NonFungibleToken.CollectionPublic,NonFungibleToken.Receiver}>(SoftCollection.CollectionPublicPath).check() {
            if account.borrow<&AnyResource>(from: SoftCollection.CollectionStoragePath) != nil {
                account.unlink(SoftCollection.CollectionPublicPath)
                account.link<&{NonFungibleToken.CollectionPublic,NonFungibleToken.Receiver}>(SoftCollection.CollectionPublicPath, target: SoftCollection.CollectionStoragePath)
            } else {
                let collection <- SoftCollection.createEmptyCollection() as! @SoftCollection.Collection
                account.save(<-collection, to: SoftCollection.CollectionStoragePath)
                account.link<&{NonFungibleToken.CollectionPublic,NonFungibleToken.Receiver}>(SoftCollection.CollectionPublicPath, target: SoftCollection.CollectionStoragePath)
            }
        }
    }
}
