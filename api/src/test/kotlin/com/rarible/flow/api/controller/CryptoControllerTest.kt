package com.rarible.flow.api.controller

import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowPublicKey
import com.nftco.flow.sdk.FlowSignature
import com.ninjasquad.springmockk.MockkBean
import com.rarible.flow.api.service.SignatureService
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@WebFluxTest(
    controllers = [SignatureController::class],
    properties = [
        "application.environment = dev",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "spring.cloud.consul.config.enabled = false",
        "logging.logstash.tcp-socket.enabled = false"
    ]
)
@AutoConfigureWebTestClient(timeout = "60000")
@ActiveProfiles("test")
internal class CryptoControllerTest {
    @Autowired lateinit var client: WebTestClient

    @MockkBean(relaxed = true)
    lateinit var flowSignatureService: SignatureService

    @Test
    fun `should respond true`() = runBlocking<Unit> {
        coEvery {
            flowSignatureService.verify(any<FlowPublicKey>(), any<FlowSignature>(), any())
        } returns true

        coEvery {
            flowSignatureService.checkPublicKey(any(), any())
        } returns true

        val pk = "528360c75ecf870d4c8a432d23710a2d0b71ac30222b55ed31e32717a6f3741dd54c07669d607f39d164d1bce6807d1c5645569bdae99e8b0c710af450aeac05"
        val signature = "e2cfa85c1539277500e8f5fcf4c85dc7aa4cbc433d43c26527d461637a5fe34d93ef7b62774dbff4f2a718a3577698aaacbab58a1fc32d40191cd5b95194505c"
        val message = "test"

        client
            .get()
            .uri("/v0.1/crypto/verify?publicKey=$pk&signature=$signature&message=$message&signerAddress=0x01")
            .exchange()
            .expectStatus().isOk
            .expectBody<Boolean>()
            .returnResult().responseBody!! shouldBe true

        coVerify(exactly = 1) {
            flowSignatureService.verify(
                FlowPublicKey(pk), FlowSignature(signature), message
            )

            flowSignatureService.checkPublicKey(
                FlowAddress("0x01"), FlowPublicKey(pk)
            )
        }
    }
}
