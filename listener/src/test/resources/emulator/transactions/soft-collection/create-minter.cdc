import NonFungibleToken from 0xNONFUNGIBLETOKEN
import SoftCollection from 0xSOFTCOLLECTION

transaction (address: Address, parentId: UInt64?, name: String, symbol: String, icon: String?, description: String?, url: String?, supply: UInt64?, royalties: {Address:UFix64}) {
    let minter: &{SoftCollection.Minter}

    prepare (account: AuthAccount) {
        self.minter = getAccount(0xSOFTCOLLECTION)
            .getCapability(SoftCollection.MinterPublicPath)
            .borrow<&{SoftCollection.Minter}>()!
    }

    execute {
        let r:[SoftCollection.Royalty] = []
        for i in royalties.keys {
            r.append(SoftCollection.Royalty(address: i, fee: royalties[i]!))
        }
        self.minter.mint(
            receiver: getAccount(address).getCapability<&{NonFungibleToken.CollectionPublic}>(SoftCollection.CollectionPublicPath),
            parentId: parentId,
            name: name,
            symbol: symbol,
            icon: icon,
            description: description,
            url: url,
            supply: nil,
            royalties: r,
        )
    }
}
