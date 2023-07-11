import NonFungibleToken from 0xf8d6e0586b0a20c7
import RaribleNFT from 0xf8d6e0586b0a20c7

transaction(metadata: String, royalties: [RaribleNFT.Royalty]) {
    let minter: Capability<&RaribleNFT.Minter>
    let receiver: Capability<&{NonFungibleToken.Receiver}>

    prepare(account: AuthAccount) {
        if account.borrow<&RaribleNFT.Collection>(from: RaribleNFT.collectionStoragePath) == nil {
            let collection <- RaribleNFT.createEmptyCollection() as! @RaribleNFT.Collection
            account.save(<- collection, to: RaribleNFT.collectionStoragePath)
            account.link<&{NonFungibleToken.CollectionPublic,NonFungibleToken.Receiver}>(RaribleNFT.collectionPublicPath, target: RaribleNFT.collectionStoragePath)
        }

        self.minter = RaribleNFT.minter()
        self.receiver = RaribleNFT.receiver(account.address)
    }

    execute {
        let minter = self.minter.borrow() ?? panic("Could not borrow receiver capability (maybe receiver not configured?)")
        minter.mintTo(creator: self.receiver, metadata: metadata, royalties: royalties)
    }
}