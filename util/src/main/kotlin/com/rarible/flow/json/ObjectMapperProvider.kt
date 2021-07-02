package com.rarible.flow.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
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

fun ObjectMapper.registerFlowModule() = this.registerModule(flowModule())

fun commonMapper() = ObjectMapper().registerKotlinModule().registerFlowModule()