package com.rarible.flow.api.controller

import com.nftco.flow.sdk.FlowPublicKey
import com.nftco.flow.sdk.FlowSignature
import com.rarible.flow.api.service.SignatureService
import com.rarible.flow.api.util.flowAddress
import com.rarible.flow.api.util.okOr404IfNull
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
        message: String
    ): ResponseEntity<Boolean> {
        val pk = FlowPublicKey(publicKey)
        val sig = FlowSignature(signature)
        val result = try {
            val sigCheck = signatureService.verify(
                pk, sig, message
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