package com.rarible.flow.api.controller

import com.rarible.flow.api.service.FlowSignatureService
import com.rarible.flow.log.Log
import com.rarible.protocol.flow.nft.api.controller.FlowNftCryptoControllerApi
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController

@CrossOrigin
@RestController
class CryptoController(
    private val signatureService: FlowSignatureService
): FlowNftCryptoControllerApi {
    override suspend fun verifySignature(
        publicKey: String,
        signerAddress: String,
        signature: String,
        message: String
    ): ResponseEntity<Boolean> {
        val sigCheck = signatureService.verify(
            publicKey, signature, message
        )
        val accountCheck = signatureService.checkPublicKey(
            signerAddress.flowAddress()!!,
            publicKey
        )

        log.debug(
            "Signature check for args=[{}, {}, {}, {}] - result: {}, account: {}",
            publicKey, signerAddress, signature, message,
            sigCheck, accountCheck
        )
        return (sigCheck && accountCheck).okOr404IfNull()
    }

    companion object {
        val log by Log()
    }

}