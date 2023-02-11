import Evolution from 0xEVOLUTIONTOKEN

pub fun main(itemId: UInt32, setId: UInt32, serial: UInt32): {String: AnyStruct?}? {
    let data: {String: AnyStruct?} = {}
    let meta = Evolution.getItemMetadata(itemId: itemId)!
    for key in meta.keys {
        data.insert(key: key, meta[key]!)
    }
    data.insert(key: "setName", Evolution.getSetName(setId: setId)!)
    data.insert(key: "setDescription", Evolution.getSetDescription(setId: setId)!)
    data.insert(key: "editions", Evolution.getNumberCollectiblesInEdition(setId: setId, itemId: itemId)!)
    return data
}
