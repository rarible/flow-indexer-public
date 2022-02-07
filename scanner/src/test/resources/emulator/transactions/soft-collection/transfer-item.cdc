import NonFungibleToken from 0xNONFUNGIBLETOKEN
import RaribleNFTv2 from 0xRARIBLENFT_V2

// transfer RaribleNFTv2 token with tokenId to given address
//
transaction(tokenId: UInt64, to: Address) {
    let token: @NonFungibleToken.NFT
    let receiver: Capability<&{NonFungibleToken.CollectionPublic}>

    prepare(account: AuthAccount) {
        let collection = account.borrow<&RaribleNFTv2.Collection>(from: RaribleNFTv2.CollectionStoragePath)
            ?? panic("could not borrow RaribleNFTv2 collection from account")
        self.token <- collection.withdraw(withdrawID: tokenId)
        self.receiver = getAccount(to).getCapability<&{NonFungibleToken.CollectionPublic}>(RaribleNFTv2.CollectionPublicPath)
    }

    execute {
        let receiver = self.receiver.borrow()
            ?? panic("recipient RaribleNFTv2 collection not initialized")
        receiver.deposit(token: <- self.token)
    }
}
