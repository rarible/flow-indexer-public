package com.rarible.flow.api

import com.google.protobuf.UnsafeByteOperations
import com.nftco.flow.sdk.*
import kotlinx.coroutines.future.await
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FlowMarketplaceBackendApp

fun main(args: Array<String>) {
    runApplication<FlowMarketplaceBackendApp>(*args)
}

suspend fun AsyncFlowAccessApi.simpleScript(block: ScriptBuilder.() -> Unit): FlowScriptResponse {
    val api = this
    val builder = flowScript(block)
    return try {
        api.executeScriptAtLatestBlock(
            script = builder.script,
            arguments = builder.arguments.map { UnsafeByteOperations.unsafeWrap(Flow.encodeJsonCadence(it)) }
        ).await()
    } catch (t: Throwable) {
        throw FlowException("Error while running script", t)
    }
}
