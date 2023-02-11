//import StarlyCard from 0x5b82f21c0edf76e3
import StarlyCard from 0xSTARLY


pub fun main(owner: Address, id: UInt64): String? {
    let col = getAccount(owner).getCapability(StarlyCard.CollectionPublicPath)
            .borrow<&{StarlyCard.StarlyCardCollectionPublic}>()
    if col == nil { return nil }

    let nft = col!.borrowStarlyCard(id: id)
    if nft == nil { return nil }
    return nft!.starlyID
}