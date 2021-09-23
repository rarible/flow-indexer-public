package com.rarible.flow.core.crypto

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId


interface FlowSignature {

    val chainId: FlowChainId

    fun verify(signer: FlowAddress, signature: String, signedMessage: ByteArray): Boolean {
        return verify(listOf(signer), listOf(signature), listOf(1000), signedMessage)
    }

    fun verify(signers: List<FlowAddress>, signatures: List<String>, weights: List<Int>, signedMessage: ByteArray): Boolean

}