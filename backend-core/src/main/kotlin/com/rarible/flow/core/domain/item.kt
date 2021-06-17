package com.rarible.flow.core.domain

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
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
    val contract: String, //Address,    // maps to `token`
    val tokenId: Int,
    val creator: Address,       // can we have multiple? maps to list of creators with one element
    val royalties: List<Part>,
    val owner: Address,
    val date: Instant,
    val blockHeight: Long,
    val meta: Map<String, String> = emptyMap()
    //val pending: List<ItemTransfer> = emptyList()
) {

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: String
        get() = makeId(this.contract, this.tokenId)
        set(_) {}

    companion object {
        fun makeId(contract: Address, tokenId: Int): String {
            return "${contract.value}:$tokenId"
        }

        fun makeId(contract: String, tokenId: Int): String {
            return "${contract}:$tokenId"
        }
    }

}

