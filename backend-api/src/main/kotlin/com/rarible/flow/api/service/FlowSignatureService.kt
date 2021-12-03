package com.rarible.flow.api.service

import com.nftco.flow.sdk.AsyncFlowAccessApi
import com.nftco.flow.sdk.FlowAccessApi
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.FlowPublicKey
import com.nftco.flow.sdk.FlowSignature
import com.nftco.flow.sdk.SignatureAlgorithm
import com.nftco.flow.sdk.cadence.BooleanField
import com.nftco.flow.sdk.cadence.marshall
import com.nftco.flow.sdk.crypto.Crypto
import com.nftco.flow.sdk.simpleFlowScript
import com.rarible.flow.api.simpleScript
import kotlinx.coroutines.future.await


class FlowSignatureService(
    private val chainId: FlowChainId,
    private val flowAccessApi: AsyncFlowAccessApi
) {

    suspend fun verify(publicKey: FlowPublicKey, signature: FlowSignature, message: String): Boolean {
        return verify(publicKey.base16Value, signature.base16Value, message)
    }

    suspend fun verify(publicKey: String, signature: String, message: String): Boolean {
        val publicKeys = marshall {
            array {
                listOf(
                    string(Crypto.decodePublicKey(publicKey, SignatureAlgorithm.ECDSA_SECP256k1).hex)
                )
            }
        }

        val weights = marshall {
            array {
                listOf(
                    ufix64(1000)
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
        return flowAccessApi
            .getAccountAtLatestBlock(account)
            .await()
            ?.let { acc ->
                acc.keys.any { key ->
                    key.publicKey == publicKey
                }
            } ?: false
    }

    private val scriptCode = this.javaClass.getResource("/script/sig_verify.cdc").readText()
}
