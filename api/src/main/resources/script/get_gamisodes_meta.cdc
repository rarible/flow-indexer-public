import MetadataViews from 0xMETADATAVIEWS
import NonFungibleToken from 0xMETADATAVIEWS

import NiftoryNonFungibleToken from 0xREGISTRY_ADDRESS
import NiftoryNFTRegistry from 0xREGISTRY_ADDRESS

pub fun main(address: Address, uuid: UInt64, storagePath: String, registryAddress: Address, brand: String) : AnyStruct {
    let obj = getAuthAccount(address).borrow<auth &AnyResource>(from: StoragePath(identifier: storagePath)!)!
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
            results[metadataType.identifier] = view?.resolveView(metadataType)!
        }
    }

    // traits
    let metaAdditional = obj as? &AnyResource{NiftoryNonFungibleToken.CollectionPublic}
    let view2 = metaAdditional?.borrow(id: uuid)
    let setId = view2?.setId
    let templateId = view2?.templateId
    let setManager = NiftoryNFTRegistry.getSetManagerPublic(registryAddress, brand)

    // Find the corresponding Template within the Set, and retrieve its metadata
    let templateMetadata = setManager
        .getSet(setId!)
        .getTemplate(templateId!)
        .metadata()
        .get() as! {String: String}

    results["Traits"] = templateMetadata
    return results
}