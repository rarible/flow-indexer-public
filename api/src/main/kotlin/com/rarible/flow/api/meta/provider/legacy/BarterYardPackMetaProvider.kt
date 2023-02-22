package com.rarible.flow.api.meta.provider.legacy

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.CadenceNamespace
import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.JsonCadenceConversion
import com.nftco.flow.sdk.cadence.JsonCadenceConverter
import com.nftco.flow.sdk.cadence.unmarshall
import com.rarible.flow.Contracts
import com.rarible.flow.api.meta.ItemMeta
import com.rarible.flow.api.meta.ItemMetaAttribute
import com.rarible.flow.api.meta.ItemMetaContent
import com.rarible.flow.api.meta.provider.ItemMetaProvider
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class BarterYardPackMetaProvider(
    @Value("\${app.chain-id}")
    private val chainId: FlowChainId,
    private val script: BarterYardScript
): ItemMetaProvider {
    override fun isSupported(itemId: ItemId): Boolean = itemId.contract == Contracts.BARTER_YARD_PACK.fqn(chainId)

    override suspend fun getMeta(item: Item): ItemMeta? {
        return script.call(item.tokenId, item.owner!!)?.toItemMeta(item.id)
    }
}


@Component
class BarterYardScript(
    private val scriptExecutor: ScriptExecutor,
    @Value("classpath:script/byp_meta.cdc")
    private val script: Resource
) {

    suspend fun call(tokenId: Long, owner: FlowAddress): Pass? {
        return scriptExecutor.executeFile(
            script,
            {
                arg { address(owner.formatted) }
                arg { uint64(tokenId) }
            },
            { optionalResult ->
                optional(optionalResult) {
                    Flow.unmarshall(Pass::class, it)
                }
            }
        )
    }
}

@JsonCadenceConversion(PassConverter::class)
data class Pass(
    val id: Long,
    val name: String,
    val description: String,
    val pack: String,
    val ipfsCID: String,
    val ipfsURI: String,
    val owner: String
): MetaBody {
    override fun toItemMeta(itemId: ItemId): ItemMeta {
        val raw = jacksonObjectMapper().writeValueAsBytes(this)
        return ItemMeta(
            itemId = itemId,
            name = name,
            description = description,
            attributes = listOf(
                ItemMetaAttribute(
                    key = "pack",
                    value = pack
                )
            ),
            contentUrls = listOf(
                "ipfs://ipfs/$ipfsCID"
            ),
            content = listOf(
                ItemMetaContent(
                    "ipfs://ipfs/$ipfsCID",
                    ItemMetaContent.Type.IMAGE,
                    ItemMetaContent.Representation.ORIGINAL,
                ),
            )
        ).apply { this.raw = raw }
    }
}

class PassConverter: JsonCadenceConverter<Pass> {
    override fun unmarshall(value: Field<*>, namespace: CadenceNamespace): Pass = unmarshall(value) {
        Pass(
            id = long("id"),
            name = string("name"),
            description = string("description"),
            pack = string("pack"),
            ipfsCID = string("ipfsCID"),
            ipfsURI = string("ipfsURI"),
            owner = address("owner")
        )
    }
}
