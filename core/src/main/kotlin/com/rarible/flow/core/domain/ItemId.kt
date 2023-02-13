package com.rarible.flow.core.domain

import java.io.Serializable

data class ItemId(val contract: String, val tokenId: TokenId, val delimiter: Char = ':') : Serializable {

    override fun toString(): String {
        return "${contract}$delimiter$tokenId"
    }

    companion object {

        fun parse(source: String, delimiter: Char = ':'): ItemId {
            val parts = if (delimiter == '.') {
                listOf(source.substringBeforeLast(delimiter), source.substringAfterLast(delimiter))
            } else source.split(delimiter)
            if (parts.size == 2) {
                val contract = parts[0]
                val tokenId = parts[1].toLong()
                return ItemId(contract, tokenId)
            } else throw IllegalArgumentException("Failed to parse ItemId from [$source] with delimiter [$delimiter]")
        }
    }
}
