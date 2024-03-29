package com.rarible.flow.api.service

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.FlowPublicKey
import com.nftco.flow.sdk.FlowSignature
import com.nftco.flow.sdk.SignatureAlgorithm
import com.nftco.flow.sdk.cadence.BooleanField
import com.nftco.flow.sdk.cadence.marshall
import com.nftco.flow.sdk.crypto.Crypto
import com.rarible.blockchain.scanner.flow.service.AsyncFlowAccessApi
import com.rarible.flow.sdk.simpleScript
import kotlinx.coroutines.future.await

class SignatureService(
    private val chainId: FlowChainId,
    private val flowAccessApi: AsyncFlowAccessApi
) {

    suspend fun verify(
        publicKey: FlowPublicKey,
        signature: FlowSignature,
        message: String,
        algorithm: SignatureAlgorithm,
        weights: Int
    ): Boolean {
        return verify(
            publicKey = publicKey.base16Value,
            signature = signature.base16Value,
            message = message,
            algorithm = algorithm,
            weight = weights
        )
    }

    private suspend fun verify(
        publicKey: String,
        signature: String,
        message: String,
        algorithm: SignatureAlgorithm,
        weight: Int
    ): Boolean {
        val publicKeys = marshall {
            array {
                listOf(
                    string(Crypto.decodePublicKey(publicKey, algorithm).hex)
                )
            }
        }

        val weights = marshall {
            array {
                listOf(
                    ufix64(weight)
                )
            }
        }

        val signatures = marshall {
            array {
                listOf(
                    string(signature)
                )
            }
        }

        val scriptResult = flowAccessApi.simpleScript {
            script(
                scriptCode, chainId
            )

            arg { publicKeys }
            arg { enum(algorithm) }
            arg { weights }
            arg { signatures }
            arg { string(message) }
        }.jsonCadence as BooleanField

        return scriptResult.value ?: false
    }

    /**
     * Returns true if account has the public key
     */
    suspend fun checkPublicKey(account: FlowAddress, publicKey: FlowPublicKey): Boolean {
        val lb = flowAccessApi.getLatestBlock(sealed = true).await()
        return flowAccessApi
            .getAccountByBlockHeight(account, height = lb.height)
            .await()
            ?.let { acc ->
                acc.keys.any { key ->
                    key.publicKey == publicKey
                }
            } ?: false
    }

    private val scriptCode = this.javaClass.getResource("/script/sig_verify.cdc")!!.readText()
}
