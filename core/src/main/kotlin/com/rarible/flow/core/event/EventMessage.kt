package com.rarible.flow.core.event

import com.nftco.flow.sdk.cadence.CadenceNamespace
import com.nftco.flow.sdk.cadence.JsonCadenceConversion
import com.nftco.flow.sdk.cadence.JsonCadenceConverter

@JsonCadenceConversion(EventMessageCadenceConverter::class)
data class EventMessage(
    val eventId: EventId,
    val fields: Map<String, com.nftco.flow.sdk.cadence.Field<*>>
)

class EventMessageCadenceConverter: JsonCadenceConverter<EventMessage> {
    override fun unmarshall(value: com.nftco.flow.sdk.cadence.Field<*>, namespace: CadenceNamespace): EventMessage =
        com.nftco.flow.sdk.cadence.unmarshall(value) {
            EventMessage(
                eventId = EventId.of(this.compositeValue.id),
                fields = this.compositeValue.fields.associate { it.name to it.value }
            )
        }
}
