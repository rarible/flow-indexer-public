package com.rarible.flow.scanner.test.contract

import com.nftco.flow.sdk.crypto.Crypto
import com.rarible.flow.scanner.test.EmulatorUser
import com.rarible.flow.scanner.test.FlowTestContainer

object RaribleNFTTestContract {

    fun init() {
        FlowTestContainer.createContract("0xRARIBLENFT")
        val account = EmulatorUser.Emulator
        val payerKey = FlowTestContainer.getAccountKey(account.address)
        val signer = Crypto.getSigner(
            privateKey = Crypto.decodePrivateKey(account.keyHex),
            hashAlgo = payerKey.hashAlgo
        )
        FlowTestContainer.execute(
            EmulatorUser.Emulator.address,
            signer,
            "/emulator/transactions/rarible_nft_init.cdc"
        )
    }
}