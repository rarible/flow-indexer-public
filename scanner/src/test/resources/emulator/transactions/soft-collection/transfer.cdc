import NonFungibleToken from "0xNONFUNGIBLETOKEN"
import SoftCollection from "0xSOFTCOLLECTION"

// transfer SoftCollection token with tokenId to given address
//
transaction(tokenId: UInt64, to: Address) {
    let token: @NonFungibleToken.NFT
    let receiver: Capability<&{NonFungibleToken.CollectionPublic}>

    prepare(account: AuthAccount) {
        let collection = account.borrow<&SoftCollection.Collection>(from: SoftCollection.CollectionStoragePath)
            ?? panic("could not borrow SoftCollection collection from account")
        self.token <- collection.withdraw(withdrawID: tokenId)
        self.receiver = getAccount(to).getCapability<&{NonFungibleToken.CollectionPublic}>(SoftCollection.CollectionPublicPath)
    }

    execute {
        let receiver = self.receiver.borrow()
            ?? panic("recipient SoftCollection collection not initialized")
        receiver.deposit(token: <- self.token)
    }
}
