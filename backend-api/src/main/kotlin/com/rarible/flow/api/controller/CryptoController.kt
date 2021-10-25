package com.rarible.flow.api.controller

import com.rarible.flow.api.service.FlowSignatureService
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
        return signatureService.verify(
            publicKey, signature, message
        ).okOr404IfNull()
    }
}