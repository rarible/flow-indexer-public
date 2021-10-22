package com.rarible.flow.api.metaprovider

import com.google.protobuf.UnsafeByteOperations
import com.nftco.flow.sdk.AsyncFlowAccessApi
import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.ScriptBuilder
import com.nftco.flow.sdk.cadence.JsonCadenceBuilder
import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.OwnershipRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.await
import kotlinx.coroutines.reactive.asFlow
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class MotoGPItemMetaProvider(
    private val api: AsyncFlowAccessApi,
    @Value("classpath:script/motogp-card-metadata.cdc")
    private val scriptFile: Resource,
    private val ownershipRepository: OwnershipRepository
) : ItemMetaProvider {

    private val cadenceBuilder = JsonCadenceBuilder()

    private val addressRegistry = Flow.DEFAULT_ADDRESS_REGISTRY

    private val builder = ScriptBuilder()

    override fun isSupported(itemId: ItemId): Boolean = /*itemId.contract.contains("MotoGPCard", true)*/false

    override suspend fun getMeta(itemId: ItemId): ItemMeta? {
        val ownership = lastKnownOwner(itemId) ?: return null
        builder.script(addressRegistry.processScript(scriptFile.file.readText(Charsets.UTF_8)))
        builder.arguments(
            mutableListOf(
                cadenceBuilder.address(ownership.owner.formatted),
                cadenceBuilder.number("UInt64", itemId.tokenId)
            )
        )
        val resp = api.executeScriptAtLatestBlock(
            builder.script,
            builder.arguments.map { UnsafeByteOperations.unsafeWrap(Flow.encodeJsonCadence(it)) }
        ).await()
        val (nft, meta) = JsonCadenceParser().array(resp.jsonCadence) {
            Pair(unmarshall<MotoGPNFT>(it.value!!.first()), unmarshall<MotoGPMeta>(it.value!!.last()))
        }

        val attributes = meta.data.filterNot { "videoUrl" == it.key }.map { e -> ItemMetaAttribute(
            key = e.key,
            value = e.value,
            type = "String",
            format = null
        ) }.toMutableList()
        attributes.add(ItemMetaAttribute(key = "uuid", value = "${nft.uuid}", type = "number", format = null))
        attributes.add(ItemMetaAttribute(key = "id", value = "${nft.id}", type = "number", format = null))
        attributes.add(ItemMetaAttribute(key = "cardID", value = "${nft.id}", type = "number", format = null))
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
}
