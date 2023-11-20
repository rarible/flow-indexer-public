package com.rarible.flow.api.controller

import com.nftco.flow.sdk.FlowPublicKey
import com.nftco.flow.sdk.FlowSignature
import com.nftco.flow.sdk.SignatureAlgorithm
import com.rarible.flow.api.service.SignatureService
import com.rarible.flow.api.util.flowAddress
import com.rarible.flow.api.util.okOr404IfNull
import com.rarible.protocol.dto.FlowSignatureAlgorithmDto
import com.rarible.protocol.flow.nft.api.controller.FlowNftCryptoControllerApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController

@CrossOrigin
@RestController
class SignatureController(
    private val signatureService: SignatureService
) : FlowNftCryptoControllerApi {

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    override suspend fun verifySignature(
        publicKey: String,
        signerAddress: String,
        signature: String,
        message: String,
        algorithm: FlowSignatureAlgorithmDto?
    ): ResponseEntity<Boolean> {
        val pk = FlowPublicKey(publicKey)
        val sig = FlowSignature(signature)
        val algo = when (algorithm) {
            FlowSignatureAlgorithmDto.ECDSA_secp56k1 -> SignatureAlgorithm.ECDSA_SECP256k1
            FlowSignatureAlgorithmDto.ECDSA_P256 -> SignatureAlgorithm.ECDSA_P256
            else -> SignatureAlgorithm.ECDSA_SECP256k1 // Default
        }
        val result = try {
            val sigCheck = signatureService.verify(
                publicKey = pk,
                signature = sig,
                message = message,
                algorithm = algo
            )
            val accountCheck = signatureService.checkPublicKey(
                signerAddress.flowAddress()!!,
                pk
            )
            logger.debug(
                "Signature check for args=[{}, {}, {}, {}] - result: {}, account: {}",
                publicKey, signerAddress, signature, message,
                sigCheck, accountCheck
            )
            sigCheck && accountCheck
        } catch (t: Throwable) {
            logger.warn(
                "Failed to check signature for args=[{}, {}, {}, {}]",
                publicKey, signerAddress, signature, message,
                t
            )
            false
        }

        return result.okOr404IfNull()
    }
}
