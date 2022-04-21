import NonFungibleToken from 0xNONFUNGIBLETOKEN
import RaribleNFT from 0xRARIBLETOKEN
import Evolution from 0xEVOLUTIONTOKEN
import MotoGPCard from 0xMOTOGPTOKEN
import TopShot from 0xTOPSHOT
import MugenNFT from 0xMUGENNFT
import CNN_NFT from 0xCNNNFT
import MatrixWorldVoucher from 0xMATRIXWORLDVOUCHER
import MatrixWorldFlowFestNFT from 0xMATRIXWORLDFLOWFEST
import Art from 0xART
import DisruptArt from 0xDISRUPTART
import RaribleNFTv2 from 0xRARIBLENFTV2
import BarterYardPackNFT from 0xBARTERYARDPACKNFT
import SomePlaceCollectible from 0xSOMEPLACECOLLECTIBLE
import GeniaceNFT from 0xGENIACENFT
import CryptoPiggo from 0xCRYPTOPIGGO

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

pub fun idsDisruptArt(_ account: PublicAccount): [UInt64] {
    return account.getCapability(DisruptArt.disruptArtPublicPath)
        .borrow<&{NonFungibleToken.CollectionPublic}>()
        ?.getIDs() ?? []
}

pub fun idsRaribleNFTv2(_ account: PublicAccount): [UInt64] {
    return account.getCapability(RaribleNFTv2.CollectionPublicPath)
        .borrow<&{NonFungibleToken.CollectionPublic}>()
        ?.getIDs() ?? []
}

pub fun idsBarterYardPack(_ account: PublicAccount): [UInt64] {
    return account.getCapability(BarterYardPackNFT.CollectionPublicPath)
        .borrow<&{NonFungibleToken.CollectionPublic}>()
        ?.getIDs() ?? []
}

pub fun idsSomePlaceCollectible(_ account: PublicAccount): [UInt64] {
    return account.getCapability(SomePlaceCollectible.CollectionPublicPath)
        .borrow<&{SomePlaceCollectible.CollectibleCollectionPublic}>()
        ?.getIDs() ?? []

}

pub fun idsGeniaceNFT(_ account: PublicAccount): [UInt64] {
    return account.getCapability(GeniaceNFT.CollectionPublicPath)
        .borrow<&{GeniaceNFT.GeniaceNFTCollectionPublic}>()
        ?.getIDs() ?? []
}

pub fun idsCryptoPiggo(_ account: PublicAccount): [UInt64] {
    return account.getCapability(CryptoPiggo.CollectionPublicPath)
        .borrow<&{CryptoPiggo.CryptoPiggoCollectionPublic}>()
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
    results["DisruptArt"] = idsDisruptArt(account)
    results["RaribleNFTv2"] = idsRaribleNFTv2(account)
    results["BarterYardPackNFT"] = idsBarterYardPack(account)
    results["SomePlaceCollectible"] = idsSomePlaceCollectible(account)
    results["GeniaceNFT"] = idsGeniaceNFT(account)
    results["CryptoPiggo"] = idsCryptoPiggo(account)

    return results
}
