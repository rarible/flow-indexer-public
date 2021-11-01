import Evolution from 0xEVOLUTIONTOKEN

pub fun main(address: Address, tokenId: UInt64): {String: UInt32} {
    let account = getAccount(address)
    let collection = getAccount(address).getCapability<&{Evolution.EvolutionCollectionPublic}>(/public/f4264ac8f3256818_Evolution_Collection).borrow()!
    let token = collection.borrowCollectible(id: tokenId)!
    return {
        "itemId": token.data.itemId,
        "setId": token.data.setId,
        "serialNumber": token.data.serialNumber
    }
}
