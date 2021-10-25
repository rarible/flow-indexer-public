package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.cadence.JsonCadenceBuilder
import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.OwnershipRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class MotoGPItemMetaProvider(
    private val scriptExecutor: ScriptExecutor,
    @Value("classpath:script/motogp-card-metadata.cdc")
    private val scriptFile: Resource,
    private val ownershipRepository: OwnershipRepository
) : ItemMetaProvider {

    private val builder = JsonCadenceBuilder()

    private lateinit var scriptText: String

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.contains("MotoGPCard", true)

    override suspend fun getMeta(itemId: ItemId): ItemMeta? {
        val ownership = lastKnownOwner(itemId) ?: return null
        val resp = scriptExecutor.execute(
            code = scriptText,
            args = mutableListOf(
                builder.address(ownership.owner.formatted),
                builder.uint64(itemId.tokenId)
            )
        )

        val (nft, meta) = JsonCadenceParser().array(resp.jsonCadence) {
            Pair(unmarshall<MotoGPNFT>(it.value!!.first()), unmarshall<MotoGPMeta>(it.value!!.last()))
        }

        val attributes = meta.data.filterNot { "videoUrl" == it.key }.map { e -> ItemMetaAttribute(
            key = e.key,
            value = e.value,
        ) }.toMutableList()
        attributes.add(ItemMetaAttribute(key = "uuid", value = "${nft.uuid}"))
        attributes.add(ItemMetaAttribute(key = "id", value = "${nft.id}"))
        attributes.add(ItemMetaAttribute(key = "cardID", value = "${nft.id}"))
        return ItemMeta(
            itemId = itemId,
            name = meta.name,
            description = meta.description,
            attributes = attributes.toList(),
            contentUrls = listOf(meta.imageUrl, meta.data["videoUrl"].orEmpty())
        )
    }

    private suspend fun lastKnownOwner(itemId: ItemId): Ownership? {
        val q = QOwnership.ownership
        val predicate = q.contract.eq(itemId.contract).and(q.tokenId.eq(itemId.tokenId))
        return ownershipRepository.findAll(predicate, q.date.desc()).asFlow().toList().maxByOrNull { it.date }
    }

    @PostConstruct
    private fun readScript() {
        scriptText = scriptFile.inputStream.bufferedReader().use { it.readText() }
    }
}
