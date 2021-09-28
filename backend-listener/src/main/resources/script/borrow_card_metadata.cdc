import NonFungibleToken from "../contracts/NonFungibleToken.cdc"
import MotoGPCard from "../contracts/MotoGPCard.cdc"
import MotoGPCardMetadata from "../contracts/MotoGPCardMetadata.cdc"

pub fun main(address: Address, tokenId: UInt64): MotoGPCardMetadata.Metadata? {
    let account = getAccount(address)
    let collection = getAccount(address).getCapability<&{MotoGPCard.ICardCollectionPublic}>(/public/motogpCardCollection).borrow()!
    let ref = collection.borrowCard(id: tokenId)!
    return ref.getCardMetadata()
}