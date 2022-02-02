package com.rarible.flow.api.service

import com.nftco.flow.sdk.AsyncFlowAccessApi
import com.nftco.flow.sdk.ScriptBuilder
import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.rarible.flow.core.config.AppProperties
import com.rarible.flow.log.Log
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

    private suspend fun <T> executeText(
        code: String,
        args: ScriptBuilder.() -> Unit,
        parse: JsonCadenceParser.(Field<*>) -> T
    ): T {
        val response = api.simpleScript {
            script(code, appProperties.chainId)
            args(this)
        }
        return parse(parser, response.jsonCadence)
    }

    suspend fun <T> executeFile(
        path: String,
        args: ScriptBuilder.() -> Unit,
        parse: JsonCadenceParser.(Field<*>) -> T
    ): T {
        val result = executeText(scriptText(path), args, parse)
        logger.info(
            "Running script {}. Result: {}",
            path,
            result
        )
        return result
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


    private fun scriptText(resourcePath: String): String {
        val resource = ClassPathResource(resourcePath)
        return scriptText(resource.inputStream)
    }

    private fun scriptText(inputStream: InputStream): String {
        return inputStream.bufferedReader().use { it.readText() }
    }

    companion object {
        val logger by Log()
    }

}
