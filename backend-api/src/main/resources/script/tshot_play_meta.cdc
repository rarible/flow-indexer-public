import TopShot from 0xTOPSHOTTOKEN

pub fun main(playID: UInt32, setID: UInt32): {String: String} {
    let res: {String: String} = TopShot.getPlayMetaData(playID: playID)!
    let setName = TopShot.getSetName(setID: setID)!
    let editions = TopShot.getNumMomentsInEdition(setID: setID, playID: playID)!
    res.insert(key: "SetName", setName)
    res.insert(key: "Editions", editions.toString())
    return res
}
