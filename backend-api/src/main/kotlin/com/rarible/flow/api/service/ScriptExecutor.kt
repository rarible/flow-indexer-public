package com.rarible.flow.api.service

import com.nftco.flow.sdk.AsyncFlowAccessApi
import com.nftco.flow.sdk.FlowScriptResponse
import com.nftco.flow.sdk.cadence.Field
import com.rarible.flow.api.simpleScript
import com.rarible.flow.core.config.AppProperties
import com.rarible.flow.log.Log
import org.bouncycastle.crypto.Digest
import org.springframework.stereotype.Service
import org.springframework.util.DigestUtils

@Service
class ScriptExecutor(
    private val api: AsyncFlowAccessApi,
    private val appProperties: AppProperties
) {

    suspend fun execute(code: String, args: MutableList<Field<*>>): FlowScriptResponse {
        val response = api.simpleScript {
            script(code, appProperties.chainId)
            arguments(args)
        }
        logger.info(
            "Running script {} with args: [{}]. Result: {}",
            DigestUtils.md5Digest(code.toByteArray()),
            logArgs(args),
            response.stringValue
        )
        return response
    }

    companion object {
        val logger by Log()

        private fun logArgs(args: List<Field<*>>): String {
            return args.foldRight("") { arg, res ->
                res + arg.value.toString() + ", "
            }
        }
    }

}
