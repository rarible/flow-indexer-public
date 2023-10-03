package com.rarible.flow.api.royalty.provider

import com.rarible.flow.Contracts
import com.rarible.flow.api.config.ApiProperties
import com.rarible.flow.api.service.ScriptExecutor
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(5)
class HWGarageTokenV2RoyaltyProvider(
    properties: ApiProperties,
    scriptExecutor: ScriptExecutor,
) : AbstractMattelRoyaltyProvider(
    Contracts.HW_GARAGE_TOKEN_V2,
    scriptExecutor,
    properties,
)
