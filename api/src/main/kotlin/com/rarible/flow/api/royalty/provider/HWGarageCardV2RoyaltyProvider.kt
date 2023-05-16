package com.rarible.flow.api.royalty.provider

import com.rarible.flow.Contracts
import com.rarible.flow.api.config.ApiProperties
import com.rarible.flow.api.service.ScriptExecutor
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(2)
class HWGarageCardV2RoyaltyProvider(
    properties: ApiProperties,
    scriptExecutor: ScriptExecutor,
) : AbstractMattelRoyaltyProvider(
    Contracts.HW_GARAGE_CARD_V2,
    scriptExecutor,
    SCRIPT_FILE,
    properties,
) {
    companion object {
        const val SCRIPT_FILE = "get_nft_metadata_HWGarageCardV2.cdc"
    }
}

