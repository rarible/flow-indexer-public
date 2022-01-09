package com.rarible.flow.api.service.flowrpc

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.domain.TokenId
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class TopShotMomentScript(
    @Value("classpath:script/get_topshot_moment.cdc")
    private val script: Resource,
    private val scriptExecutor: ScriptExecutor,
) {

    suspend fun call(owner: FlowAddress, tokenId: TokenId): Map<String, Long> {
        return scriptExecutor.executeFile(
            script,
            {
                arg { address(owner.bytes) }
                arg { uint64(tokenId) }
            }, { json ->
                dictionaryMap(json) { k, v ->
                    string(k) to long(v)
                }
            })
    }
}