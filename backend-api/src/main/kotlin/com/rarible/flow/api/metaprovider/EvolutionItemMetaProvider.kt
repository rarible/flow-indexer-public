package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.rarible.flow.Contracts
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.json.JacksonJsonParser
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class EvolutionItemMetaProvider(
    @Value("classpath:script/evo_meta.cdc")
    private val scriptFile: Resource,
    private val scriptExecutor: ScriptExecutor
): ItemMetaProvider {

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.contains(Contracts.EVOLUTION.contractName)

    override suspend fun getMeta(item: Item): ItemMeta? {
        if (item.meta.isNullOrEmpty()) return null
        val meta = JacksonJsonParser().parseMap(item.meta)
        val data: Map<String, Field<*>> = scriptExecutor.executeFile(scriptFile, {
            arg {uint32(meta["itemId"].toString())}
            arg {uint32(meta["setId"].toString())}
            arg {uint32(meta["serialNumber"].toString())}
        }, { json ->
            optional(json) {
                dictionaryMap(it) { k, v -> string(k) to v }
            }
        }) ?: return null

        val jsonCadenceParser = JsonCadenceParser() // TODO parse proper structure

        val attributes = listOf(
            ItemMetaAttribute(
                key =  "hash",
                value = data["Hash"]?.let { jsonCadenceParser.optional(it) { string(it) } }
            ),
            ItemMetaAttribute(
                key = "setName",
                value = data["setName"]?.let { jsonCadenceParser.optional(it) { string(it) } }
            ),
            ItemMetaAttribute(
                key = "itemId",
                value = meta["itemId"]?.toString()
            ),
            ItemMetaAttribute(
                key = "setId",
                value = meta["setId"]?.toString()
            ),
            ItemMetaAttribute(
                key = "serialNumber",
                value = meta["serialNumber"]?.toString()
            ),
            ItemMetaAttribute(
                key = "setDescription",
                value = data["setDescription"]?.let { jsonCadenceParser.optional(it) { string(it) } }
            ),
            ItemMetaAttribute(
                key = "editions",
                value = data["editions"]?.let { jsonCadenceParser.optional(it) { int(it).toString() } }
            )
        )

        val contents = listOf(
            "https://storage.viv3.com/0xf4264ac8f3256818/${meta["itemId"].toString()}"
        )
        return ItemMeta(
            itemId = item.id,
            name = data["Title"]?.let { jsonCadenceParser.optional(it) { string(it) } }.orEmpty(),
            description = data["Description"]?.let { jsonCadenceParser.optional(it) { string(it) } }.orEmpty(),
            attributes = attributes,
            contentUrls = contents,
            content = listOf(
                ItemMeta.Content(
                    contents.single(),
                    ItemMeta.Content.Representation.ORIGINAL,
                    ItemMeta.Content.Type.IMAGE,
                )
            ),
        ).apply {
            raw = toString().toByteArray(Charsets.UTF_8)
        }
    }

}
