package com.rarible.flow.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rarible.flow.events.EventId
import com.rarible.flow.events.EventMessage
import com.rarible.flow.jackson.EventIdJackson


fun flowModule(): SimpleModule {
    val module = SimpleModule()
    module.addDeserializer(EventMessage::class.java, FlowEventDeserializer())
    module.addSerializer(EventId::class.java, EventIdJackson.Serializer())
    module.addDeserializer(EventId::class.java, EventIdJackson.Deserializer())
    return module
}

fun commonMapper(): ObjectMapper =  ObjectMapper()
    .registerKotlinModule()
    .registerModule(flowModule())
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
