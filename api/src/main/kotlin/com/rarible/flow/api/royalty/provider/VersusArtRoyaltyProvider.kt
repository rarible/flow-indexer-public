package com.rarible.flow.api.royalty.provider

import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.cadence.JsonCadenceBuilder
import com.nftco.flow.sdk.cadence.ResourceField
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.event.VersusArtItem
import com.rarible.flow.core.event.changeCapabilityToAddress
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
@Order(1)
class VersusArtRoyaltyProvider(
    private val scriptExecutor: ScriptExecutor,
    @Value("classpath:script/versus_art_meta.cdc")
    private val scriptFile: Resource,
) : ItemRoyaltyProvider {

    private val builder = JsonCadenceBuilder()

    private val scriptText: String =
        scriptFile.inputStream.bufferedReader().use { it.readText() }

    override fun isSupported(itemId: ItemId): Boolean =
        itemId.contract.substringAfterLast(".") == "Art"

    override suspend fun getRoyalties(item: Item): List<Royalty> {
        val result = scriptExecutor.execute(
            scriptText,
            mutableListOf(
                builder.address(item.owner!!.formatted),
                builder.uint64(item.tokenId)
            ),
        ).let { it.copy(bytes = it.bytes.changeCapabilityToAddress()) }
        val value = result.jsonCadence.value as ResourceField
        val nft = Flow.unmarshall(VersusArtItem::class, value)
        return nft.royalty.map { Royalty(it.value.wallet, it.value.cut) }
    }

}
