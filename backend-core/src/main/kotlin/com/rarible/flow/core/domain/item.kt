package com.rarible.flow.core.domain

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@JvmInline
value class Address(val value: String)

@JvmInline
value class TxHash(val value: String)

data class Part(
    val address: Address,
    val fee: Int
)

data class ItemTransfer(
    val from: Address,
    val to: Address,
    val txHash: TxHash
)

@Document
data class Item(
    val collection: Address,    // maps to `token`
    val tokenId: Int,
    val creator: Address,       // can we have multiple? maps to list of creators with one element
    val royalties: List<Part>,
    val owner: Address,
    val date: Instant,
    val blockHeight: Long,
    val meta: Map<String, String> = emptyMap()
    //val pending: List<ItemTransfer> = emptyList()
)

private fun parseEvent(eventType: String, eventData: String, blockHeight: Long): Item? {
    val parts = eventType.split('.')
    return if(parts.size > 1 && parts.first() == "A" && parts.last() == "Mint") {
        try {
            val payload = ObjectMapper().readTree(eventData)
            val fields = payload["value"]["fields"]
            val contract = parts[1]
            val creator = Address(fieldValue(fields, "owner"))
            val meta = fields.filter { it["name"].textValue() != "id" && it["name"].textValue() != "owner" }.map {
                it["name"].textValue() to it["value"]["value"].textValue()
            }.toMap()

            Item(
                Address(contract),
                Integer.valueOf(fieldValue(fields, "id")),
                creator,
                emptyList(),
                creator,
                Instant.now(),
                blockHeight,
                meta
            )
        } catch (e: Exception) {
            null
        }
    } else null
}

private fun fieldValue(fields: JsonNode, fieldName: String): String {
    return fields.first { it["name"].textValue() == fieldName }["value"]["value"].textValue()
}

