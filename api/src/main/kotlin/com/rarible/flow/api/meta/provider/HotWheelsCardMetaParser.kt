package com.rarible.flow.api.meta.provider

import com.rarible.flow.core.domain.ItemId

object HotWheelsCardMetaParser : MattelMetaParser() {

    override fun getName(map: Map<String, String>, itemId: ItemId): String? {
        return map.get("carName") + " #" + map.get("cardId")
    }

    override val fieldName = fields("carName")
    override val fieldDescription = fields()
    override val fieldImageOriginal = fields("imageUrl", "imageCID")
    override val fieldRights = fields("licensorLegal")

}