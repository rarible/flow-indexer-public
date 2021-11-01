import NonFungibleToken from 0xNONFUNGIBLETOKEN
import RaribleNFT from 0xRARIBLETOKEN
import Evolution from 0xEVOLUTIONTOKEN
import MotoGPCard from 0xMOTOGPTOKEN
import TopShot from 0xTOPSHOTTOKEN

pub fun idsCommonNFT(_ account: PublicAccount): [UInt64] {
    return account.getCapability(RaribleNFT.collectionPublicPath)
        .borrow<&{NonFungibleToken.CollectionPublic}>()
        ?.getIDs() ?? []
}

pub fun idsEvolution(_ account: PublicAccount): [UInt64] {
    return account.getCapability(/public/f4264ac8f3256818_Evolution_Collection)
        .borrow<&{Evolution.EvolutionCollectionPublic}>()
        ?.getIDs() ?? []
}

pub fun idsMotoGpCard(_ account: PublicAccount): [UInt64] {
    return account.getCapability(/public/motogpCardCollection)
        .borrow<&{MotoGPCard.ICardCollectionPublic}>()
        ?.getIDs() ?? []
}

pub fun idsTopShot(_ account: PublicAccount): [UInt64] {
    return account.getCapability(/public/MomentCollection)
        .borrow<&{TopShot.MomentCollectionPublic}>()
        ?.getIDs() ?? []
}

pub fun main(address: Address): {String: [UInt64]} {
    let account = getAccount(address)
    let results : {String: [UInt64]} = {}

    results["RaribleNFT"] = idsCommonNFT(account)
    results["Evolution"] = idsEvolution(account)
    results["MotoGPCard"] = idsMotoGpCard(account)
    results["TopShot"] = idsTopShot(account)

    return results
}
