package com.rarible.flow.listener.handler.listeners

import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.events.BlockInfo
import org.onflow.sdk.FlowAddress

/**
 * Handles smart-contracts events
 *
 * Inheritors should be registered as @Components with bean name corresponding to cadence event name
 * e.g. `@Component("TopShot.Minted")`.
 * All handlers are supposed to be autowired as Map of (eventName) -> (handlerInstance)
 */
interface SmartContractEventHandler<T> {
    suspend fun handle(contract: String, tokenId: TokenId, fields: Map<String, Any?>, blockInfo: BlockInfo): T
}
