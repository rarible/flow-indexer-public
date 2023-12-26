package com.rarible.flow.api.royalty.provider

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.Contracts
import com.rarible.flow.api.service.ScriptExecutor
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(8)
class BarbieTokenRoyaltyProvider(
    chainId: FlowChainId,
    scriptExecutor: ScriptExecutor,
) : AbstractMattelRoyaltyProvider(
    Contracts.BARBIE_TOKEN,
    scriptExecutor,
    chainId,
)
