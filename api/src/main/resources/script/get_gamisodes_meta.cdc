import MetadataViews from 0xMETADATAVIEWS

pub fun main(address: Address, uuid: UInt64) : AnyStruct {
    let obj = getAuthAccount(address).borrow<auth &AnyResource>(from: StoragePath(identifier: "GamisodesCollection")!)!
    let meta = obj as? &AnyResource{MetadataViews.ResolverCollection}

    let results : {String:AnyStruct} = {}

    var view = meta?.borrowViewResolver(id: uuid)
    if let views = view?.getViews() {
        for metadataType in views {
            if metadataType == Type<MetadataViews.NFTView>() {
                continue
            }
            if metadataType == Type<MetadataViews.NFTCollectionData>() {
                continue
            }
            if metadataType == Type<MetadataViews.NFTCollectionDisplay>() {
                continue
            }
            results[metadataType.identifier] = view?.resolveView(metadataType)
        }
    }       
    return results
}