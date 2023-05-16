package com.rarible.flow.api.service

import com.nftco.flow.sdk.AsyncFlowAccessApi
import com.nftco.flow.sdk.FlowScriptResponse
import com.nftco.flow.sdk.ScriptBuilder
import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.rarible.flow.core.config.AppProperties
import com.rarible.flow.core.util.Log
import com.rarible.flow.sdk.simpleScript
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class ScriptExecutor(
    private val api: AsyncFlowAccessApi,
    private val appProperties: AppProperties
) {
    private val parser = JsonCadenceParser()

    suspend fun execute(code: String, args: MutableList<Field<*>>): FlowScriptResponse {
        val response = api.simpleScript {
            script(code, appProperties.chainId)
            arguments(args)
        }
        return response
    }

    private fun scriptText(inputStream: InputStream): String {
        return inputStream.bufferedReader().use { it.readText() }
    }

    suspend fun <T> executeFile(
        resourcePath: String,
        args: ScriptBuilder.() -> Unit,
        parse: JsonCadenceParser.(Field<*>) -> T,
        processResponse: FlowScriptResponse.() -> FlowScriptResponse = {this}
    ): T {
        return executeFile(ClassPathResource(resourcePath), args, parse)
    }

    suspend fun <T> executeFile(
        resource: Resource,
        args: ScriptBuilder.() -> Unit,
        parse: JsonCadenceParser.(Field<*>) -> T
    ): T {
        val result = executeText(scriptText(resource.inputStream), args, parse)
        logger.info(
            "Running script {}. Result: {}",
            resource.filename,
            result
        )
        return result
    }

    private suspend fun <T> executeText(
        code: String,
        args: ScriptBuilder.() -> Unit,
        parse: JsonCadenceParser.(Field<*>) -> T,
        processResponse: FlowScriptResponse.() -> FlowScriptResponse = {this}
    ): T {
        val response = api.simpleScript {
            script(code, appProperties.chainId)
            args(this)
        }

        return parse(parser, processResponse(response).jsonCadence)
    }

    companion object {
        val logger by Log()
    }

}
