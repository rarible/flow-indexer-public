//import Art from 0xd796ff17107bbff6
import Art from 0xART

// Take Art token content by account address and tokenId
//
pub fun main(address: Address, tokenId: UInt64): String? {
    let collection = getAccount(address)
        .getCapability(Art.CollectionPublicPath)
        .borrow<&{Art.CollectionPublic}>()
    if collection == nil { return nil }

    let nft = collection!.borrowArt(id: tokenId)
    if nft == nil { return nil }
    return nft!.content()
}
