import BarterYardPackNFT from 0xBARTER_YARD_PACK
import MetadataViews from 0xMETADATAVIEWS

pub struct Pass {
    pub let id: UInt64
    pub let name: String
    pub let description: String
    pub let edition: UInt16
    pub let ipfsCID: String
    pub let ipfsURI: String
    pub let owner: Address

    init(id: UInt64, name: String, edition: UInt16, description: String, ipfsCID: String, ipfsURI: String, owner: Address) {
        self.id = id
        self.name = name
        self.description = description
        self.edition = edition
        self.ipfsCID = ipfsCID
        self.ipfsURI = ipfsURI
        self.owner = owner
    }
}

pub fun main(address: Address, id: UInt64): Pass? {
    let collection = getAccount(address).getCapability(BarterYardPackNFT.CollectionPublicPath)
        .borrow<&{ BarterYardPackNFT.BarterYardPackNFTCollectionPublic }>()
        ?? panic("Could not borrow a reference to the collection")

    var id: UInt64 = 0
    var name: String = ""
    var edition: UInt16 = 0
    var description: String = ""
    var ipfsCID: String = ""
    var ipfsURI: String = ""
    var owner: Address = address

    if let nft = collection.borrowBarterYardPackNFT(id: id) {
        // Get the basic display information for this NFT
        if let view = nft.resolveView(Type<MetadataViews.Display>()) {
            let display = view as! MetadataViews.Display
            let ipfsFile = display.thumbnail as! MetadataViews.IPFSFile

            id = id
            name = display.name
            description = display.description
            ipfsCID = ipfsFile.cid
            ipfsURI = ipfsFile.uri()
        }

        if let packPartView = nft.resolveView(Type<BarterYardPackNFT.PackMetadataDisplay>()) {
            let packMetadata = packPartView as! BarterYardPackNFT.PackMetadataDisplay

            edition = packMetadata.edition
        }

        return Pass(
            id: id,
            name: name,
            edition: edition,
            description: description,
            ipfsCID: ipfsCID,
            ipfsURI: ipfsURI,
            owner: owner
        )
    }
    return nil
}
