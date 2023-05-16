package com.rarible.flow.api.royalty.provider

import com.rarible.flow.Contracts
import com.rarible.flow.api.config.ApiProperties
import com.rarible.flow.api.service.ScriptExecutor
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(8)
class BarbieTokenRoyaltyProvider(
    properties: ApiProperties,
    scriptExecutor: ScriptExecutor,
) : AbstractMattelRoyaltyProvider(
    Contracts.BARBIE_TOKEN,
    scriptExecutor,
    SCRIPT_FILE,
    properties,
) {
    companion object {
        const val SCRIPT_FILE = "get_nft_metadata_BBxBarbieToken.cdc"
    }
}