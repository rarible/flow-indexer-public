import OneFootballCollectible from 0x6831760534292098
import NonFungibleToken from 0x1d7e57aa55817448
import OneFootballCollectible from 0xONEFOOTBALL
import NonFungibleToken from 0xNONFUNGIBLETOKEN

pub struct NFTData {
    pub let id: UInt64
    pub let templateID: UInt64
    pub let seriesName: String
    pub let name: String
    pub let description: String
    pub let preview: String
    pub let media: String
    pub let data: {String: String}

    init(nft: &OneFootballCollectible.NFT) {
        let metadata = nft.getTemplate()!

        self.id = nft.id
        self.templateID = nft.templateID
        self.seriesName = metadata.seriesName
        self.name = metadata.name
        self.description = metadata.description
        self.preview = "https://".concat(metadata.preview).concat(".ipfs.dweb.link/")
        self.media = "https://".concat(metadata.media).concat(".ipfs.dweb.link/")
        self.data = metadata.data
    }
}

pub fun main(owner: Address, nftID: UInt64): NFTData? {
    let account = getAccount(owner)

    if let collection = account
        .getCapability<&OneFootballCollectible.Collection{NonFungibleToken.CollectionPublic, OneFootballCollectible.OneFootballCollectibleCollectionPublic}>(OneFootballCollectible.CollectionPublicPath)
            .borrow() {
        if let nft = collection.borrowOneFootballCollectible(id: nftID) {
            if let metadata = nft.getTemplate() {
                return NFTData(nft: nft)
            }
        }
    }

    return nil
}