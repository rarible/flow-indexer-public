import MetadataViews from 0xMETADATAVIEWS
import #CONTRACNAME from 0xCONTRACTADDRESS


pub struct NFTView {
    pub let id: UInt64
    pub let uuid: UInt64
    pub let name: String
    pub let royalties: [MetadataViews.Royalty]

    init(
        id: UInt64,
        uuid: UInt64,
        name: String,
        royalties: [MetadataViews.Royalty],
    ) {
        self.id = id
        self.uuid = uuid
        self.name = name
        self.royalties = royalties
    }
}

pub fun main(address: Address, id: UInt64): NFTView {
    let account = getAccount(address)

    let collection = account
        .getCapability(#CONTRACNAME.CollectionPublicPath)
        .borrow<&{MetadataViews.ResolverCollection}>()
        ?? panic("Could not borrow a reference to the collection")

    let viewResolver = collection.borrowViewResolver(id: id)!

    let nftView = MetadataViews.getNFTView(id: id, viewResolver : viewResolver)

    let collectionSocials: {String: String} = {}
    for key in nftView.collectionDisplay!.socials.keys {
        collectionSocials[key] = nftView.collectionDisplay!.socials[key]!.url
    }


    return NFTView(
        id: nftView.id,
        uuid: nftView.uuid,
        name: nftView.display!.name,
        royalties: nftView.royalties!.getRoyalties()
    )
}