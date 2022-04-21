import TopShot from 0xTOPSHOT

pub fun main(account: Address, id: UInt64): {String: UInt32} {
    // get the public capability for the owner's moment collection
    // and borrow a reference to it
    let collectionRef = getAccount(account).getCapability(/public/MomentCollection)
        .borrow<&{TopShot.MomentCollectionPublic}>()
        ?? panic("Could not get public moment collection reference")

    // Borrow a reference to the specified moment
    let token = collectionRef.borrowMoment(id: id)
        ?? panic("Could not borrow a reference to the specified moment")
    return {
        "playID" : token.data.playID,
        "setID": token.data.setID,
        "serialNumber": token.data.serialNumber
    }
}
