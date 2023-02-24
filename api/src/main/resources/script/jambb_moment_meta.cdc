//import Moments from 0xd4ad4740ee426334
import Moments from 0xJAMBBMOMENTS

pub fun main(id: UInt64): Moments.MomentMetadata? {
    let cc = Moments.getContentCreator()
    return cc.getMomentMetadata(momentID:id)
}