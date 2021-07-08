package com.rarible.flow.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.rarible.flow.events.EventMessage
import org.onflow.sdk.FlowEvent
import java.time.LocalDateTime
import java.time.ZoneOffset

class FlowEventDeserializer : JsonDeserializer<EventMessage>() {

    override fun deserialize(parser: JsonParser, ctx: DeserializationContext): EventMessage {
        val obj: JsonNode = parser.codec.readTree(parser)
        FlowEvent
        val e = obj["value"]
        val id = e["id"].asText()
        val fields = e.get("fields").asIterable().associate {
            val name = it["name"].asText()
            val type = it["value"]["type"].asText()

            val value = if ("Optional" == type) {
                it["value"]["value"]["value"]?.asText("")
            } else {
                it["value"]["value"]?.asText("")
            }
            name to value
        }
        return EventMessage(
            id = id,
            fields = fields,
            timestamp = LocalDateTime.now(ZoneOffset.UTC)
        )
    }
}
