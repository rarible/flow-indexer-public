//import ChainmonstersRewards from 0x93615d25d14fa337
import ChainmonstersRewards from 0xCHAINMONSTERS

pub fun main(owner: Address, tokenId: UInt64): UInt32? {
    let col = getAccount(owner).getCapability(/public/ChainmonstersRewardCollection)
            .borrow<&{ChainmonstersRewards.ChainmonstersRewardCollectionPublic}>()
    if col == nil { return nil }

    let nft = col!.borrowReward(id: tokenId)
    if(nft == nil) { return nil }
    return nft!.data.rewardID
}