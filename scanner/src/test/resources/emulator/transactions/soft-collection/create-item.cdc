import NonFungibleToken from 0xNONFUNGIBLETOKEN
import SoftCollection from 0xSOFTCOLLECTION
import RaribleNFTv2 from 0xRARIBLENFT_V2

transaction(minterId: UInt64, meta: RaribleNFTv2.Meta, royalties: [RaribleNFTv2.Royalty]) {
    let minter: &SoftCollection.Collection
    let receiver: Capability<&{NonFungibleToken.CollectionPublic}>

    prepare (account: AuthAccount) {
        self.minter = account.borrow<&SoftCollection.Collection>(from: SoftCollection.CollectionStoragePath)!
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
        self.receiver = account.getCapability<&{NonFungibleToken.CollectionPublic}>(RaribleNFTv2.CollectionPublicPath)
    }

    execute {
        self.minter.mint(
            softId: minterId,
            receiver: self.receiver,
            meta: meta,
            royalties: royalties,
        )
    }
}
