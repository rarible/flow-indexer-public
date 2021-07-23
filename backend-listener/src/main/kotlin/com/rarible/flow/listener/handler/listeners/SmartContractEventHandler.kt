package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.TokenId
import org.onflow.sdk.FlowAddress

interface SmartContractEventHandler<T> {
    suspend fun handle(contract: FlowAddress, tokenId: TokenId, fields: Map<String, Any?>): T
}