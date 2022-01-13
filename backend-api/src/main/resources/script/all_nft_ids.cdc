import NonFungibleToken from 0xNONFUNGIBLETOKEN
import RaribleNFT from 0xRARIBLETOKEN
import Evolution from 0xEVOLUTIONTOKEN
import MotoGPCard from 0xMOTOGPTOKEN
import TopShot from 0xTOPSHOTTOKEN
import MugenNFT from 0xMUGENNFT
import CNN_NFT from 0xCNNNFT
import MatrixWorldVoucher from 0xMATRIXWORLD
import MatrixWorldFlowFestNFT from 0xMATRIXWORLDFLOWFEST
import Art from 0xVERSUSART

pub fun idsRaribleNFT(_ account: PublicAccount): [UInt64] {
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

pub fun idsMugenNFT(_ account: PublicAccount): [UInt64] {
    return account.getCapability(MugenNFT.CollectionPublicPath)
        .borrow<&{NonFungibleToken.CollectionPublic}>()
        ?.getIDs() ?? []
}

pub fun idsCnnNFT(_ account: PublicAccount): [UInt64] {
    return account.getCapability(CNN_NFT.CollectionPublicPath)
        .borrow<&{NonFungibleToken.CollectionPublic}>()
        ?.getIDs() ?? []
}

pub fun idsMatrixWorld(_ account: PublicAccount): [UInt64] {
    return account.getCapability(MatrixWorldVoucher.CollectionPublicPath)
        .borrow<&{NonFungibleToken.CollectionPublic}>()
        ?.getIDs() ?? []
}

pub fun idsMatrixWorldFlowFest(_ account: PublicAccount): [UInt64] {
    return account.getCapability(MatrixWorldFlowFestNFT.CollectionPublicPath)
        .borrow<&{NonFungibleToken.CollectionPublic}>()
        ?.getIDs() ?? []
}

pub fun idsVersusArt(_ account: PublicAccount): [UInt64] {
    return account.getCapability(Art.CollectionPublicPath)
        .borrow<&{Art.CollectionPublic}>()
        ?.getIDs() ?? []
}

pub fun main(address: Address): {String: [UInt64]} {
    let account = getAccount(address)
    let results : {String: [UInt64]} = {}

    results["RaribleNFT"] = idsRaribleNFT(account)
    results["Evolution"] = idsEvolution(account)
    results["MotoGPCard"] = idsMotoGpCard(account)
    results["TopShot"] = idsTopShot(account)
    results["MugenNFT"] = idsMugenNFT(account)
    results["CNN_NFT"] = idsCnnNFT(account)
    results["MatrixWorld"] = idsMatrixWorld(account)
    results["MatrixWorldFlowFestNFT"] = idsMatrixWorldFlowFest(account)
    results["VersusArt"] = idsVersusArt(account)

    return results
}
