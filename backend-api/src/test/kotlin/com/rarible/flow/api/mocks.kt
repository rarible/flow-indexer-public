package com.rarible.flow.api

import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.FlowScript
import com.nftco.flow.sdk.FlowScriptResponse
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.config.AppProperties
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
            mockk("scriptExecutor") {
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

    fun resource(scriptExecutorKey: String, fileName: String? = null) = mockk<Resource>() {
        every {
            inputStream
        } returns ByteArrayInputStream(scriptExecutorKey.toByteArray())

        every {
            filename
        } returns (fileName ?: scriptExecutorKey)
    }

    fun webClient(expectedPath: String, response: String) = WebClient
        .builder()
        .exchangeFunction { req ->
            req.url().toString() shouldBe expectedPath

            Mono.just(
                ClientResponse.create(HttpStatus.OK)
                    .header("content-type", "application/json")
                    .body(response)
                    .build()
            )
        }.build()
}