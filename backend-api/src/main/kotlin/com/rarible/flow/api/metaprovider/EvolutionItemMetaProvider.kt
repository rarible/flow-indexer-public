package com.rarible.flow.api.metaprovider

import com.nftco.flow.sdk.cadence.Field
import com.nftco.flow.sdk.cadence.JsonCadenceBuilder
import com.nftco.flow.sdk.cadence.JsonCadenceParser
import com.rarible.flow.api.service.ScriptExecutor
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import com.rarible.flow.core.repository.ItemRepository
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.json.JacksonJsonParser
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class EvolutionItemMetaProvider(
    @Value("classpath:script/evo_meta.cdc")
    private val scriptFile: Resource,
    private val itemRepository: ItemRepository,
    private val scriptExecutor: ScriptExecutor
): ItemMetaProvider {

    private val cadenceBuilder = JsonCadenceBuilder()

    override fun isSupported(itemId: ItemId): Boolean = itemId.contract.contains("Evolution")

    override suspend fun getMeta(itemId: ItemId): ItemMeta? {
        val item = itemRepository.findById(itemId).awaitSingleOrNull() ?: return null
        if (item.meta.isNullOrEmpty()) return null
        val meta = JacksonJsonParser().parseMap(item.meta)
        val resp = scriptExecutor.execute(
            code = scriptFile.file.readText(Charsets.UTF_8),
            args = mutableListOf(
                cadenceBuilder.uint32(meta["itemId"].toString()),
                cadenceBuilder.uint32(meta["setId"].toString()),
                cadenceBuilder.uint32(meta["serialNumber"].toString())
            )
        )

        val jsonCadenceParser = JsonCadenceParser()
        val data: Map<String, Field<*>> = jsonCadenceParser.optional(resp.jsonCadence) {
            dictionaryMap(it) { k, v -> string(k) to v }
        } ?: return null

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
                value = meta["itemId"] as String?
            ),
            ItemMetaAttribute(
                key = "setId",
                value = meta["setId"] as String?
            ),
            ItemMetaAttribute(
                key = "serialNumber",
                value = meta["serialNumber"] as String?
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
            itemId = itemId,
            name = data["Title"]?.let { jsonCadenceParser.optional(it) { string(it) } }.orEmpty(),
            description = data["Description"]?.let { jsonCadenceParser.optional(it) { string(it) } }.orEmpty(),
            attributes = attributes,
            contentUrls = contents,
        ).apply {
            raw = toString().toByteArray(Charsets.UTF_8)
        }
    }
}
