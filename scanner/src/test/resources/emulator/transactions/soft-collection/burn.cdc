import NonFungibleToken from 0xNONFUNGIBLETOKEN
import SoftCollection from 0xSOFTCOLLECTION

// Burn SoftCollection on signer account by tokenId
//
transaction(tokenId: UInt64) {
    prepare(account: AuthAccount) {
        let collection = account.borrow<&SoftCollection.Collection>(from: SoftCollection.CollectionStoragePath)
            ?? panic("could not borrow SoftCollection collection from account")
        destroy collection.withdraw(withdrawID: tokenId)
    }
}
