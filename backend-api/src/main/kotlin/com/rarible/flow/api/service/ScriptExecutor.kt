package com.rarible.flow.api.service

import com.nftco.flow.sdk.AsyncFlowAccessApi
import com.nftco.flow.sdk.FlowScriptResponse
import com.nftco.flow.sdk.cadence.Field
import com.rarible.flow.api.simpleScript
import com.rarible.flow.core.config.AppProperties
import org.springframework.stereotype.Service

@Service
class ScriptExecutor(
    private val api: AsyncFlowAccessApi,
    private val appProperties: AppProperties
) {

    suspend fun execute(code: String, args: MutableList<Field<*>>): FlowScriptResponse = api.simpleScript {
        script(code, appProperties.chainId)
        arguments(args)
    }
}
