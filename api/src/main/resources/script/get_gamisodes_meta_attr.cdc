import NiftoryNFTRegistry from 0xREGISTRY_ADDRESS

pub fun main(registryAddress: Address, brand: String, setId: Int, templateId: Int,) : AnyStruct{
    let setManager = NiftoryNFTRegistry.getSetManagerPublic(registryAddress, brand)

    // Find the corresponding Template within the Set, and retrieve its metadata
    let templateMetadata = setManager
        .getSet(setId!)
        .getTemplate(templateId!)
        .metadata()
        .get() as! {String: String}

    return templateMetadata
}