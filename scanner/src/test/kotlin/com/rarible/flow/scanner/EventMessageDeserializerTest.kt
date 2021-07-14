package com.rarible.flow.scanner

import com.fasterxml.jackson.module.kotlin.readValue
import com.rarible.flow.events.EventMessage
import com.rarible.flow.json.commonMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * Created by TimochkinEA at 08.06.2021
 */
class EventMessageDeserializerTest {

    @ParameterizedTest
    @ValueSource(
        strings = [
            "{\"type\":\"Event\",\"value\":{\"id\":\"A.e3750a9bc4137f3f.DisruptNow.Withdraw\",\"fields\":[{\"name\":\"id\",\"value\":{\"type\":\"UInt64\",\"value\":\"34\"}},{\"name\":\"from\",\"value\":{\"type\":\"Optional\",\"value\":{\"type\":\"Address\",\"value\":\"0x9b86289236e7fe76\"}}}]}}",
            "{\"type\":\"Event\",\"value\":{\"id\":\"A.e3750a9bc4137f3f.Marketplace.ForSale\",\"fields\":[{\"name\":\"id\",\"value\":{\"type\":\"UInt64\",\"value\":\"34\"}},{\"name\":\"price\",\"value\":{\"type\":\"UFix64\",\"value\":\"1.00000000\"}}]}}",
            "{\"type\":\"Event\",\"value\":{\"id\":\"A.7e60df042a9c0868.FlowToken.TokensWithdrawn\",\"fields\":[{\"name\":\"amount\",\"value\":{\"type\":\"UFix64\",\"value\":\"0.00010000\"}},{\"name\":\"from\",\"value\":{\"type\":\"Optional\",\"value\":{\"type\":\"Address\",\"value\":\"0xf086a545ce3c552d\"}}}]}}",
            "{\"type\":\"Event\",\"value\":{\"id\":\"A.e4e5f90bf7e2a25f.NFTProvider.Mint\",\"fields\":[{\"name\":\"id\",\"value\":{\"type\":\"UInt64\",\"value\":\"211\"}},{\"name\":\"collection\",\"value\":{\"type\":\"String\",\"value\":\"A.e4e5f90bf7e2a25f.NFTProvider.NFT\"}},{\"name\":\"creator\",\"value\":{\"type\":\"Address\",\"value\":\"0xf23ff23d90720ab4\"}},{\"name\":\"royalties\",\"value\":{\"type\":\"Array\",\"value\":[{\"type\":\"Struct\",\"value\":{\"id\":\"A.e4e5f90bf7e2a25f.NFTProvider.Royalties\",\"fields\":[{\"name\":\"address\",\"value\":{\"type\":\"Address\",\"value\":\"0xf23ff23d90720ab4\"}},{\"name\":\"fee\",\"value\":{\"type\":\"UInt8\",\"value\":\"10\"}}]}}]}},{\"name\":\"metadata\",\"value\":{\"type\":\"Struct\",\"value\":{\"id\":\"A.e4e5f90bf7e2a25f.NFTProvider.Metadata\",\"fields\":[{\"name\":\"uri\",\"value\":{\"type\":\"String\",\"value\":\"https://i.pinimg.com/originals/65/17/22/651722a77be5cd72e194660e264896b8.png\"}},{\"name\":\"title\",\"value\":{\"type\":\"String\",\"value\":\"A.e4e5f90bf7e2a25f.NFTProvider.NFT\"}},{\"name\":\"description\",\"value\":{\"type\":\"Optional\",\"value\":{\"type\":\"String\",\"value\":\"A.e4e5f90bf7e2a25f.NFTProvider.NFT\"}}},{\"name\":\"properties\",\"value\":{\"type\":\"Dictionary\",\"value\":[]}}]}}}]}}\n"

        ]
    )
    fun deserializeEventWithFieldsTest(source: String) {
        val mapper = commonMapper()
        val raw = mapper.readTree(source)
        val expectedId = raw["value"]["id"].asText()
        val message = mapper.readValue<EventMessage>(source)
        assertNotNull(message)
        assertEquals(expectedId, message.id, "ID not equals!")
        assertTrue(message.fields.isNotEmpty())
    }
}
