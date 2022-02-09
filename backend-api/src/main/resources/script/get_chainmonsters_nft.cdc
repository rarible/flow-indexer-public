//import ChainmonstersRewards from 0x93615d25d14fa337
import ChainmonstersRewards from 0xCHAINMONSTERS

pub fun main(owner: Address, id: UInt64): &AnyResource? {
    let col = getAccount(owner)
        .getCapability(/public/ChainmonstersRewardCollection)
        .borrow<&{ChainmonstersRewards.ChainmonstersRewardCollectionPublic}>()
    if col == nil { return nil }

    return col!.borrowReward(id: id)
}