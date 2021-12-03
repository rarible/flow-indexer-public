import FungibleToken from 0xFUNGIBLETOKENADDRESS
import FUSD from 0xFUSDTOKENADDRESS

pub fun main(accounts: [Address]): {Address: UFix64} {

    let result: {Address: UFix64} = {}
    for acc in accounts {
        let balance = getAccount(acc)
            .getCapability(/public/fusdBalance)
            .borrow<&FUSD.Vault{FungibleToken.Balance}>()?.balance
            ?: 0.0
        result[acc] = balance
    }

    return result
}