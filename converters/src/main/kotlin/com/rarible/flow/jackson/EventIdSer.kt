package com.rarible.flow.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer
import com.rarible.flow.events.EventId


object EventIdJackson {
    class Serializer: StdScalarSerializer<EventId>(EventId::class.java) {
        override fun serialize(value: EventId, gen: JsonGenerator, provider: SerializerProvider) {
            gen.writeString(
                value.toString()
            )
        }
    }

    class Deserializer: StdScalarDeserializer<EventId>(EventId::class.java) {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): EventId {
            return EventId.of(p.valueAsString)
        }

    }
}