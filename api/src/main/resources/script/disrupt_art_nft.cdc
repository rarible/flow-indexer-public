import DisruptArt from 0xDISRUPTART
import NonFungibleToken from 0xNONFUNGIBLETOKEN

//[UInt64] &DisruptNow.NFT
pub fun main(owner:Address, tokenid:UInt64): &NonFungibleToken.NFT {
    let collectionRef = getAccount(owner)
        .getCapability(DisruptArt.disruptArtPublicPath)
        .borrow<&{DisruptArt.DisruptArtCollectionPublic}>()
        ?? panic("Could not borrow capability from public collection")

    let nft = collectionRef.borrowNFT(id:tokenid)
    return nft
}
