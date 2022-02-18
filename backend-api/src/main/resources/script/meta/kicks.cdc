//import Kicks from 0xf3cc54f4d91c2f6c
//import Kicks from 0xKICKS

//pub struct Meta {
//    pub let title: String?
//    pub let description: String?
//    pub let media: [String]
//    pub let metadata: {String: AnyStruct}
//
//    init(
//        title: String?,
//        description: String?,
//        media: [String],
//        metadata: {String: AnyStruct}
//    ) {
//        self.title = title
//        self.description = description
//        self.media = media
//        self.metadata = metadata
//    }
//}

//pub fun main(owner: Address, id: UInt64): Meta? {
// pub fun main(owner: Address, id: UInt64): String? {
//     let col = getAccount(owner)
//       .getCapability(Kicks.CollectionPublicPath)
//       .borrow<&{Kicks.KicksCollectionPublic}>()
//
//     if col == nil { return nil }
//
//     let nft = col!.borrowSneaker(id: id)
//     if nft == nil { return nil }
// return "hi"
//
//    return nft!.name()

//    let metadata = nft!.getMetadata()
//    var media: [String] = []
//
//    if let mediaValue = metadata["media"] {
//        if let supportedMedia = mediaValue as? {String: [String]} {
//            for mediaType in supportedMedia.keys {
//                for mediaURI in supportedMedia[mediaType]! {
//                    media.append(mediaURI)
//                }
//            }
//        }
//    }
//
//    return Meta(
//        title: nft!.name(),
//        description: nft!.description(),
//        media: media,
//        metadata: metadata,
//    )
//}

import Kicks from 0xf3cc54f4d91c2f6c // TESTNET: 0xe861e151d3556d70

// Gets a Sneaker NFT's name, description and media URLs

pub fun main(account: Address, id: UInt64): NFTData {
    let collectionRef = getAccount(account)
        .getCapability(Kicks.CollectionPublicPath)
        .borrow<&{Kicks.KicksCollectionPublic}>()
        ?? panic("Could not borrow capability from public collection")

    let nft = collectionRef.borrowSneaker(id: id) ?? panic("Could not borrow Sneaker")

    let metadata = nft.getMetadata()
    var images: [String] = []
    var videos: [String] = []

    if let mediaValue = metadata["media"] {
        if let supportedMedia = mediaValue as? {String: [String]} {
            if let images = supportedMedia["image"] {
                images.appendAll(images)
            }
            if let videos = supportedMedia["video"] {
                videos.appendAll(videos)
            }
        }
    }

    return NFTData(name: nft.name(),
                description: nft.description(),
                externalURL: "https://www.nftlx.io/nft/".concat(nft.id.toString()),
                imageURLs: images,
                videoURLs: videos,
                metadata: metadata)
}

pub struct NFTData {
    pub let name: String
    pub let description: String

    pub let externalUrl: String

    pub let imageURLs: [String]
    pub let videoURLs: [String]

    pub let metadata: {String: AnyStruct}

    init(name: String, description: String, externalURL: String, imageURLs: [String], videoURLs: [String], metadata: {String: AnyStruct}) {
        self.name = name
        self.description = description
        self.externalUrl = externalURL
        self.imageURLs = imageURLs
        self.videoURLs = videoURLs
        self.metadata = metadata
    }
}