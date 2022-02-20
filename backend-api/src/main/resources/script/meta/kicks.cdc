import Kicks from 0xf3cc54f4d91c2f6c
//import Kicks from 0xKICKS

pub struct Meta {
    pub let title: String
    pub let description: String
    pub let media: [String]
    pub let redeemed: Bool
    pub let taggedTopShot: String?
    pub let metadata: {String: AnyStruct}

    init(
        title: String,
        description: String,
        media: [String],
        redeemed: Bool,
        taggedTopShot: String?,
        metadata: {String: AnyStruct}
    ) {
        self.title = title
        self.description = description
        self.media = media
        self.redeemed = redeemed
        self.taggedTopShot = taggedTopShot
        self.metadata = metadata
    }
}

pub fun main(owner: Address, id: UInt64): Meta? {
     let col = getAccount(owner)
       .getCapability(Kicks.CollectionPublicPath)
       .borrow<&{Kicks.KicksCollectionPublic}>()

     if col == nil { return nil }

     let nft = col!.borrowSneaker(id: id)
     if nft == nil { return nil }

    let metadata = nft!.getMetadata()
    var images: [String] = []
    var videos: [String] = []
    var media: [String] = []

    if let mediaValue = metadata["media"] {
        if let supportedMedia = mediaValue as? {String: [String]} {

            if let images = supportedMedia["image"] {
                images.appendAll(images)
            }
            if let videos = supportedMedia["video"] {
                videos.appendAll(videos)
            }
        } else {
            panic("nothing1")
        }
    } else {
        panic("nothing")
    }

    

    media.appendAll(videos)
    media.appendAll(images)


    let taggedTopShot = metadata["taggedTopShot"] as? String
    let redeemed = metadata["redeemed"] as? Bool

    return Meta(
        title: nft!.name(),
        description: nft!.description(),
        media: media,
        redeemed: redeemed ?? false,
        taggedTopShot: taggedTopShot,
        metadata: metadata
    )
}