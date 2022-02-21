import Kicks from 0xf3cc54f4d91c2f6c
//import Kicks from 0xKICKS

pub struct Meta {
    pub let title: String
    pub let description: String
    pub let metadata: {String: AnyStruct}

    init(
        title: String,
        description: String,
        metadata: {String: AnyStruct}
    ) {
        self.title = title
        self.description = description
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

    return Meta(
        title: nft!.name(),
        description: nft!.description(),
        metadata: metadata
    )
}