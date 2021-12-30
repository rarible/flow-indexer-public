import MatrixWorldFlowFestNFT from 0xMATRIXWORLDFLOWFEST

pub fun main(address: Address, tokenId: UInt64): MatrixWorldFlowFestNFT.Metadata? {
     if let col = getAccount(address).getCapability(MatrixWorldFlowFestNFT.CollectionPublicPath)
        .borrow<&{MatrixWorldFlowFestNFT.MatrixWorldFlowFestNFTCollectionPublic}>() {
            return col.borrowVoucher(id: tokenId)?.metadata
        } else {
            return nil
        }
}