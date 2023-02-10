package com.rarible.flow.sdk

import com.google.protobuf.UnsafeByteOperations
import com.nftco.flow.sdk.AsyncFlowAccessApi
import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowException
import com.nftco.flow.sdk.FlowScriptResponse
import com.nftco.flow.sdk.ScriptBuilder
import com.nftco.flow.sdk.flowScript
import kotlinx.coroutines.future.await

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