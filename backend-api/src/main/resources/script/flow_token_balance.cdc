import FungibleToken from 0xFUNGIBLETOKENADDRESS
import FlowToken from 0xFLOWTOKENADDRESS

pub fun main(accounts: [Address]): {Address: UFix64} {
    let result: {Address: UFix64} = {}
    for acc in accounts {
        let balance = getAccount(acc)
            .getCapability(/public/flowTokenBalance)
            .borrow<&FlowToken.Vault{FungibleToken.Balance}>()?.balance
            ?: 0.0
        result[acc] = balance
    }

    return result
}