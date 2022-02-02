package com.rarible.flow.api

import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.FlowScript
import com.nftco.flow.sdk.FlowScriptResponse
import com.rarible.flow.api.metaprovider.CnnNFTMetaProviderTest
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.config.AppProperties
import com.rarible.flow.log.Log
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.io.ByteArrayInputStream
import java.util.concurrent.CompletableFuture

object mocks {
    fun scriptExecutor(results: Map<String, String>): ScriptExecutor {
        return ScriptExecutor(
            mockk() {
                results.forEach { (script, response) ->
                    every {
                        executeScriptAtLatestBlock(eq(FlowScript(script)), any())
                    } returns CompletableFuture.completedFuture(
                        FlowScriptResponse(response.toByteArray())
                    )
                }
            },
            AppProperties("test", "", FlowChainId.EMULATOR)
        )
    }

    fun scriptExecutor(vararg results: Pair<String, String>): ScriptExecutor {
        return scriptExecutor(mapOf(*results))
    }

    fun resource(script: String, fileName: String? = null) = mockk<Resource>() {
        every {
            inputStream
        } returns ByteArrayInputStream(script.toByteArray())

        every {
            filename
        } returns (fileName ?: script)
    }

    fun webClient(expectedPath: String, response: String) = WebClient.builder()
        .exchangeFunction { req ->
            req.url().path shouldBe expectedPath

            Mono.just(
                ClientResponse.create(HttpStatus.OK)
                    .header("content-type", "application/json")
                    .body(response)
                    .build()
            )
        }.build()
}
