import SomePlaceCollectible from 0xSOMEPLACECOLLECTIBLE
import NonFungibleToken from 0xNONFUNGIBLETOKEN

pub struct NFTData {
    pub let id: UInt64
    pub let title: String
    pub let description: String
    pub let mediaUrl: String
    pub let attributes: {String: String}

    init(id: UInt64, title: String, description: String, mediaUrl: String, attributes: {String: String}) {
        self.id = id
        self.title = title
        self.description = description
        self.mediaUrl = mediaUrl
        self.attributes = attributes
    }
}

pub fun main(owner: Address, id: UInt64): NFTData? {
    let acc = getAccount(owner)

    let col = acc.getCapability(SomePlaceCollectible.CollectionPublicPath)
        .borrow<&{SomePlaceCollectible.CollectibleCollectionPublic}>()
    if col == nil { return nil }

    let optNft = col!.borrowCollectible(id: id)
    if optNft == nil { return nil }
    let nft = optNft!

    let setID = nft.setID
    let setMetadata = SomePlaceCollectible.getMetadataForSetID(setID: setID)!
    let editionMetadata = SomePlaceCollectible.getMetadataForNFTByUUID(uuid: nft.id)!

    return NFTData(
        id: id,
        title: editionMetadata.getMetadata()["title"] ?? setMetadata.getMetadata()["title"] ?? "",
        description: editionMetadata.getMetadata()["description"] ?? setMetadata.getMetadata()["description"] ?? "",
        mediaUrl: editionMetadata.getMetadata()["mediaURL"] ?? setMetadata.getMetadata()["mediaURL"] ?? "",
        attributes: {
            "editionNumber": nft.editionNumber.toString(),
            "editionCount": setMetadata.getMaxNumberOfEditions().toString(),
        }
    )

}
