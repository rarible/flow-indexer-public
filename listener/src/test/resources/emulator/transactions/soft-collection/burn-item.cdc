import NonFungibleToken from 0xNONFUNGIBLETOKEN
import RaribleNFTv2 from 0xRARIBLENFTV2

// Burn RaribleNFTv2 on signer account by tokenId
//
transaction(tokenId: UInt64) {
    prepare(account: AuthAccount) {
        let collection = account.borrow<&RaribleNFTv2.Collection>(from: RaribleNFTv2.CollectionStoragePath)
            ?? panic("could not borrow RaribleNFTv2 collection from account")
        destroy collection.withdraw(withdrawID: tokenId)
    }
}
