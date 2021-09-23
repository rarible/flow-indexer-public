package com.rarible.flow.api.service

import com.nftco.flow.sdk.*
import com.nftco.flow.sdk.cadence.BooleanField
import com.nftco.flow.sdk.cadence.marshall
import com.nftco.flow.sdk.crypto.Crypto


class FlowSignatureService(
    val scriptCode: String,
    val chainId: FlowChainId,
    val flowAccessApi: FlowAccessApi
) {

    fun verify(publicKey: String, signature: String, message: String): Boolean {

        val publicKeys = marshall {
            array {
                listOf(
                    string(Crypto.decodePublicKey(publicKey).hex)
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

        val scriptResult = flowAccessApi.simpleFlowScript {

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
}