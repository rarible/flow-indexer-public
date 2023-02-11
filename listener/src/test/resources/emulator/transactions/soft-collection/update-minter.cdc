import SoftCollection from 0xSOFTCOLLECTION

transaction(id: UInt64, url: String?, description: String?, icon: String?) {
    let minter: &SoftCollection.Collection

    prepare (account: AuthAccount) {
        self.minter = account.borrow<&SoftCollection.Collection>(from: SoftCollection.CollectionStoragePath)!
    }

    execute {
        self.minter.updateItem(id: id, icon: icon, description: description, url: url)
    }
}
