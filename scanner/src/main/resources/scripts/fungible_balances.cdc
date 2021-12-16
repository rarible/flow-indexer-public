import FungibleToken from 0xFUNGIBLETOKEN
import FlowToken from 0xFLOWTOKEN
import FUSD from 0xFUSDTOKEN

pub struct Balance {
    pub var account: Address
    pub var token: String
    pub var amount: UFix64

    pub init(account: Address, token: String, amount: UFix64) {
        self.account = account
        self.token = token
        self.amount = amount
    }
}

pub fun main(accounts: [Address]): [Balance] {
    var res: [Balance] = []
    for acc in accounts {
        let a = getAccount(acc)

        let flowBalance = a
            .getCapability(/public/flowTokenBalance)
            .borrow<&FlowToken.Vault{FungibleToken.Balance}>()?.balance
            ?? 0.0

        res.append(
            Balance(account: acc, token: Type<FlowToken>().identifier, amount: flowBalance)
        )

        let fusdBalance = a
            .getCapability(/public/fusdBalance)
            .borrow<&FUSD.Vault{FungibleToken.Balance}>()?.balance
            ?? 0.0

        res.append(
            Balance(account: acc, token: Type<FUSD>().identifier, amount: fusdBalance)
        )
    }

    return res
}