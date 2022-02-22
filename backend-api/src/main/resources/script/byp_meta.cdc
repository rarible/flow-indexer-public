import BarterYardPackNFT from 0xBARTERYARDPACKNFT
import MetadataViews from 0xMETADATAVIEWS

pub struct Pass {
    pub let id: UInt64
    pub let name: String
    pub let description: String
    pub let pack: String
    pub let ipfsCID: String
    pub let ipfsURI: String
    pub let owner: Address

    init(id: UInt64, name: String, description: String, pack: String, ipfsCID: String, ipfsURI: String, owner: Address) {
        self.id = id
        self.name = name
        self.description = description
        self.pack = pack
        self.ipfsCID = ipfsCID
        self.ipfsURI = ipfsURI
        self.owner = owner
    }
}

pub fun main(address: Address, id: UInt64): Pass? {
    let collection = getAccount(address).getCapability(BarterYardPackNFT.CollectionPublicPath)
        .borrow<&{ BarterYardPackNFT.BarterYardPackNFTCollectionPublic }>()
        ?? panic("Could not borrow a reference to the collection")

    if let nft = collection.borrowBarterYardPackNFT(id: id) {
        var name: String = ""
        var edition: UInt16 = 0
        var pack: String = ""
        var description: String = ""
        var ipfsCID: String = ""
        var ipfsURI: String = ""
        var owner: Address = address

        // Get the basic display information for this NFT
        if let view = nft.resolveView(Type<MetadataViews.Display>()) {
            let display = view as! MetadataViews.Display
            let ipfsFile = display.thumbnail as! MetadataViews.IPFSFile

            name = display.name
            description = display.description
            ipfsCID = ipfsFile.cid
            ipfsURI = ipfsFile.uri()
        }

        if let packPartView = nft.resolveView(Type<BarterYardPackNFT.PackMetadataDisplay>()) {
            let packMetadata = packPartView as! BarterYardPackNFT.PackMetadataDisplay

            edition = packMetadata.edition
            pack = BarterYardPackNFT.getPackPartById(packPartId: packMetadata.packPartId)!.name
        }

        return Pass(
            id: id,
            name: name.concat(" #").concat(edition.toString()),
            description: description,
            pack: pack,
            ipfsCID: ipfsCID,
            ipfsURI: ipfsURI,
            owner: owner
        )
    }
    return nil
}
