import NonFungibleToken from 0xNONFUNGIBLETOKEN
import SoftCollection from 0xSOFTCOLLECTION
import RaribleNFTv2 from 0xRARIBLENFT_V2

transaction(minterId: UInt64, receiver: Address, meta: RaribleNFTv2.Meta, royalties: [RaribleNFTv2.Royalty]) {
    let minter: &SoftCollection.Collection

    prepare (account: AuthAccount) {
        self.minter = account.borrow<&SoftCollection.Collection>(from: SoftCollection.CollectionStoragePath)!
    }

    execute {
        self.minter.mint(
            softId: minterId,
            receiver: getAccount(receiver).getCapability<&{NonFungibleToken.CollectionPublic}>(RaribleNFTv2.CollectionPublicPath),
            meta: meta,
            royalties: royalties,
        )
    }
}
