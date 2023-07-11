import NonFungibleToken from 0xf8d6e0586b0a20c7
import RaribleNFT from 0xf8d6e0586b0a20c7

transaction {
    prepare(account: AuthAccount) {
        if account.borrow<&RaribleNFT.Collection>(from: RaribleNFT.collectionStoragePath) == nil {
            let collection <- RaribleNFT.createEmptyCollection() as! @RaribleNFT.Collection
            account.save(<- collection, to: RaribleNFT.collectionStoragePath)
            account.link<&{NonFungibleToken.CollectionPublic,NonFungibleToken.Receiver}>(RaribleNFT.collectionPublicPath, target: RaribleNFT.collectionStoragePath)
        }
    }
}