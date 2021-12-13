import NonFungibleToken from 0xNONFUNGIBLETOKEN
import CNN_NFT from 0xCNNNFT

// Take CNN_NFT token props by account address and tokenId
//
pub fun main(address: Address, tokenId: UInt64): &AnyResource {
    return CNN_NFT.fetch(address, tokenId)
}
