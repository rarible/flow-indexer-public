package com.rarible.flow.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.rarible.flow.events.BlockInfo
import com.rarible.flow.events.EventId
import com.rarible.flow.events.EventMessage
import java.time.LocalDateTime
import java.time.ZoneOffset

class FlowEventDeserializer : JsonDeserializer<EventMessage>() {

    override fun deserialize(parser: JsonParser, ctx: DeserializationContext): EventMessage {
        val obj: JsonNode = parser.codec.readTree(parser)

        val e = obj["value"]
        val id = e["id"].asText()
        val fields = parseFields(e.get("fields"))
        return EventMessage(
            eventId = EventId.of(id),
            fields = fields,
            timestamp = LocalDateTime.now(ZoneOffset.UTC),
            BlockInfo()
        )
    }

    private fun parseFields(e: JsonNode): Map<String, Any?> = e.asIterable().associate {
        val name:String = it["name"].asText()

        val value: Any? = when (it["value"]["type"].asText()) {
            "Optional" -> {
                it["value"]["value"]["value"]?.asText("")
            }
            "Struct" -> {
                val struct = it["value"]
                parseFields(struct["value"]["fields"])
            }
            else -> {
                it["value"]["value"]?.asText("")
            }
        }
        name to value
    }
}
