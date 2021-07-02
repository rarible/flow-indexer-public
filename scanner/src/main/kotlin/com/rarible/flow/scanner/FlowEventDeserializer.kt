package com.rarible.flow.scanner

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.rarible.flow.events.EventMessage
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Created by TimochkinEA at 08.06.2021
 */
class FlowEventDeserializer : JsonDeserializer<EventMessage>() {

    override fun deserialize(parser: JsonParser, ctx: DeserializationContext): EventMessage {
        val obj: JsonNode = parser.codec.readTree(parser)
        val e = obj["value"]
        val id = e["id"].asText()
        val fields = e.get("fields").asIterable().associate {
            val name = it["name"].asText()
            val type = it["value"]["type"].asText()

            val value = if ("Optional" == type) {
                it["value"]["value"]["value"]
            } else {
                it["value"]["value"]
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
