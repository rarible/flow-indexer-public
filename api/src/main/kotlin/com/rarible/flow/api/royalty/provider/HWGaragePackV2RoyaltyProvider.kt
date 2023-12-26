package com.rarible.flow.api.royalty.provider

import com.nftco.flow.sdk.FlowChainId
import com.rarible.flow.Contracts
import com.rarible.flow.api.service.ScriptExecutor
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(5)
class HWGaragePackV2RoyaltyProvider(
    chainId: FlowChainId,
    scriptExecutor: ScriptExecutor,
) : AbstractMattelRoyaltyProvider(
    Contracts.HW_GARAGE_PACK_V2,
    scriptExecutor,
    chainId,
)
