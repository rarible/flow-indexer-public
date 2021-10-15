package com.rarible.flow.scanner.eventlisteners

import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta

interface NftMetaExtractor {

    suspend fun supported(contractName: String): Boolean

    suspend fun extract(itemId: ItemId): ItemMeta?
}
