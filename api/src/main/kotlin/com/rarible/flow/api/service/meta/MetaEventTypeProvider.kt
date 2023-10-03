package com.rarible.flow.api.service.meta

import com.rarible.flow.core.domain.ItemId

interface MetaEventTypeProvider {

    fun getMetaEventType(itemId: ItemId): MetaEventType?
}
