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

        val value: Any? = parseValue(it["value"])
        name to value
    }

    private fun parseValue(json: JsonNode): Any? = when (json["type"].asText()) {
        "Optional" -> {
            json["value"]["value"]?.asText("")
        }
        "Struct" -> {
            val struct = json["value"]
            parseFields(struct["fields"])
        }
        "Array" -> {
            json["value"].map { element ->
                parseValue(element)
            }.toList()
        }
        "Type" -> {
            json["value"]["staticType"]
        }
        else -> {
            json["value"]?.asText("")
        }
    }
}
