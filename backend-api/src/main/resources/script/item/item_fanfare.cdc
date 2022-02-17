//import FanfareNFTContract from 0x4c44f3b1e4e70b20
import FanfareNFTContract from 0xFANFARE

pub fun main(owner: Address, id: UInt64): String? {
     if let col = getAccount(owner)
        .getCapability<&{FanfareNFTContract.FanfareNFTCollectionPublic}>(FanfareNFTContract.CollectionPublicPath)
        .borrow() {
            return col!.borrowNFTMetadata(id: id)?.metadata
        } else {
            return nil
        }
}