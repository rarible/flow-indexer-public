package com.rarible.flow.api.service.flowrpc

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.api.service.ScriptExecutor
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class ScanUserNftScript(
    @Value("classpath:script/all_nft_ids.cdc")
    private val script: Resource,
    private val scriptExecutor: ScriptExecutor,
) {
    suspend fun call(address: FlowAddress): Map<String, List<Long>> {
        return scriptExecutor.executeFile(
            script,
            {
                arg { address(address.bytes) }
            },
            { json ->
                dictionaryMap(json) { k, v ->
                    string(k) to arrayValues(v) { field ->
                        long(field)
                    }
                }
            }
        )
    }
}
