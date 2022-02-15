package com.rarible.flow.api.metaprovider

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.rarible.flow.Contracts
import com.rarible.flow.api.config.ApiProperties
import com.rarible.flow.api.metaprovider.body.MetaBody
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.ItemMetaAttribute
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.coFindById
import org.springframework.boot.json.JacksonJsonParser
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Component
class FanfareMetaProvider(
    private val itemRepository: ItemRepository,
    private val apiProperties: ApiProperties
): ItemMetaProvider {
    val parser = JacksonJsonParser()
    val objectMapper = jacksonObjectMapper()

    override fun isSupported(itemId: ItemId): Boolean =
        itemId.contract == Contracts.FANFARE.fqn(apiProperties.chainId)

    override suspend fun getMeta(itemId: ItemId): ItemMeta {
        return itemRepository
            .coFindById(itemId)
            ?.meta
            ?.let { parser.parseMap(it) }
            ?.let { map -> map["metadata"] as String }
            ?.let { meta -> objectMapper.readValue<FanfareMeta>(meta)}
            ?.toItemMeta(itemId) ?: ItemMeta.empty(itemId)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class FanfareMeta(
    val name: String,
    val description: String,
    val attributes: List<FanfareAttribute>,
    @get:JsonProperty("audio_url")
    val audioUrl: String,
    val image: String,

): MetaBody {
    override fun toItemMeta(itemId: ItemId): ItemMeta {
        return ItemMeta(
            itemId, name, description,
            attributes.map(FanfareAttribute::toItemAttribute),
            listOf(
                image, audioUrl
            )
        )
    }
}

data class FanfareAttribute(
    @get:JsonProperty("display_type")
    val displayType: String?,
    @get:JsonProperty("trait_type")
    val traitType: String?,
    val value: String?
) {
    fun toItemAttribute(): ItemMetaAttribute {
        return if(displayType == "date") {
            ItemMetaAttribute(
                traitType!!,
                DateTimeFormatter.ISO_LOCAL_DATE.format(
                    Instant.ofEpochSecond(value!!.toLong()).atOffset(ZoneOffset.UTC)
                )
            )
        } else ItemMetaAttribute(traitType!!, value!!)
    }
}