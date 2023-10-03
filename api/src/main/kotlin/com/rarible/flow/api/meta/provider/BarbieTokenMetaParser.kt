package com.rarible.flow.api.meta.provider

import com.rarible.flow.api.meta.getFirst
import com.rarible.flow.core.domain.ItemId

object BarbieTokenMetaParser : MattelMetaParser() {

    override fun getName(map: Map<String, String>, itemId: ItemId): String? {
        return map.getFirst(*fieldName) + " #" + (map.get("cardId") ?: map.get("cardID"))
    }

    override val fieldName = fields("name")
    override val fieldDescription = fields()
    override val fieldImageOriginal = fields("imageCID", "tokenImageHash")
    override val fieldRights = fields("licensorLegal")
}
