//import ChainmonstersRewards from 0x93615d25d14fa337
import ChainmonstersRewards from 0xCHAINMONSTERS

pub struct Meta {
    pub let rewardId: UInt32
    pub let title: String?

    init(rewardId: UInt32, title: String?) {
        self.rewardId = rewardId
        self.title = title
    }
}

pub fun main(owner: Address, tokenId: UInt64): Meta? {
    let col = getAccount(owner).getCapability(/public/ChainmonstersRewardCollection)
            .borrow<&{ChainmonstersRewards.ChainmonstersRewardCollectionPublic}>()
    if col == nil { return nil }

    let nft = col!.borrowReward(id: tokenId)
    if(nft == nil) { return nil }
    return Meta(
        rewardId: nft!.data.rewardID,
        title: ChainmonstersRewards.getRewardMetaData(rewardID: nft!.data.rewardID)
    )
}