import NonFungibleToken from 0xNONFUNGIBLETOKEN
import RaribleNFTv2 from 0xRARIBLENFTV2

// Setup storage for RaribleNFTv2 on signer account
//
transaction {
    prepare(account: AuthAccount) {
        if !account.getCapability<&{NonFungibleToken.CollectionPublic,NonFungibleToken.Receiver}>(RaribleNFTv2.CollectionPublicPath).check() {
            if account.borrow<&AnyResource>(from: RaribleNFTv2.CollectionStoragePath) != nil {
                account.unlink(RaribleNFTv2.CollectionPublicPath)
                account.link<&{NonFungibleToken.CollectionPublic,NonFungibleToken.Receiver}>(RaribleNFTv2.CollectionPublicPath, target: RaribleNFTv2.CollectionStoragePath)
            } else {
                let collection <- RaribleNFTv2.createEmptyCollection() as! @RaribleNFTv2.Collection
                account.save(<-collection, to: RaribleNFTv2.CollectionStoragePath)
                account.link<&{NonFungibleToken.CollectionPublic,NonFungibleToken.Receiver}>(RaribleNFTv2.CollectionPublicPath, target: RaribleNFTv2.CollectionStoragePath)
            }
        }
    }
}
