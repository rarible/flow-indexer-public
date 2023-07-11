import NonFungibleToken from 0xf8d6e0586b0a20c7
import RaribleNFT from 0xf8d6e0586b0a20c7

transaction(tokenId: UInt64) {
    prepare(account: AuthAccount) {
        if account.borrow<&RaribleNFT.Collection>(from: RaribleNFT.collectionStoragePath) == nil {
            let collection <- RaribleNFT.createEmptyCollection() as! @RaribleNFT.Collection
            account.save(<- collection, to: RaribleNFT.collectionStoragePath)
            account.link<&{NonFungibleToken.CollectionPublic,NonFungibleToken.Receiver}>(RaribleNFT.collectionPublicPath, target: RaribleNFT.collectionStoragePath)
        }
        let collection = account.borrow<&RaribleNFT.Collection>(from: RaribleNFT.collectionStoragePath)!!
        destroy collection.withdraw(withdrawID: tokenId)
    }
}