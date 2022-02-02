package com.rarible.flow.api.royaltyprovider

import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.cadence.JsonCadenceBuilder
import com.nftco.flow.sdk.cadence.ResourceField
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.events.VersusArtItem
import com.rarible.flow.events.changeCapabilityToAddress
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class VersusArtRoyaltyProvider(
    private val scriptExecutor: ScriptExecutor,
    @Value("classpath:script/versus-art-metadata.cdc")
    private val scriptFile: Resource,
) : ItemRoyaltyProvider {

    private val builder = JsonCadenceBuilder()

    private val scriptText: String =
        scriptFile.inputStream.bufferedReader().use { it.readText() }

    override fun isSupported(itemId: ItemId): Boolean =
        itemId.contract.contains("VersusArt")

    override suspend fun getRoyalty(item: Item): List<Royalty> {
        val nft = scriptExecutor.executeFile(
            scriptFile,
            {
                arg { address(item.owner!!.formatted) }
                arg { ufix64(item.tokenId) }
            },
            {
                (it.value as ResourceField?)?.let { it1 -> Flow.unmarshall(VersusArtItem::class, it1) }
            },
            {
                this.copy(bytes = this.bytes.changeCapabilityToAddress())
            }
        ) ?: return emptyList()
        return nft.royalty.map { Royalty(it.value.wallet, it.value.cut) }
    }

}
