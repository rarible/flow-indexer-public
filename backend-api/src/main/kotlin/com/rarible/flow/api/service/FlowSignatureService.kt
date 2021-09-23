package com.rarible.flow.api.service

import com.nftco.flow.sdk.*
import com.nftco.flow.sdk.cadence.BooleanField
import com.nftco.flow.sdk.cadence.marshall
import com.nftco.flow.sdk.crypto.Crypto
import com.nftco.flow.sdk.crypto.PublicKey


class FlowSignatureService(
    val chainId: FlowChainId,
    val flowAccessApi: FlowAccessApi
) {

    fun verify(publicKey: String, signature: String, message: String): Boolean {

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

    val scriptCode = """
        import Crypto

        pub fun main(rawPublicKeys: [String], weights: [UFix64], signatures: [String], signedData: String): Bool {
          let keyList = Crypto.KeyList()
          var i = 0
          for rawPublicKey in rawPublicKeys {
            keyList.add(
              PublicKey(
                publicKey: rawPublicKey.decodeHex(),
                signatureAlgorithm: SignatureAlgorithm.ECDSA_secp256k1 //SignatureAlgorithm.ECDSA_P256  
              ),
              hashAlgorithm: HashAlgorithm.SHA3_256,
              weight: weights[i],
            )
            i = i + 1
          }

          let signatureSet: [Crypto.KeyListSignature] = []
          var j = 0
          for signature in signatures {
            signatureSet.append(
              Crypto.KeyListSignature(
                keyIndex: j,
                signature: signature.decodeHex()
              )
            )
            j = j + 1
          }

          return keyList.verify(
            signatureSet: signatureSet,
            signedData: signedData.decodeHex(),
          )
        }
    """.trimIndent()
}