package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.ItemId
import java.time.Instant

sealed interface Continuation

data class NftItemContinuation(
    val afterDate: Instant,
    val afterId: ItemId
): Continuation {
    override fun toString(): String {
        return "${afterDate.epochSecond}$SEPARATOR$afterId"
    }

    companion object {
        const val SEPARATOR = '_'

        fun parse(str: String?): NftItemContinuation? {
            return if(str == null || str.isEmpty()) {
                null
            } else {

                if(str.contains(SEPARATOR)) {
                    val (dateStr, idStr) = str.split(SEPARATOR)
                    NftItemContinuation(Instant.ofEpochSecond(dateStr.toLong()), ItemId.parse(idStr))
                } else {
                    null
                }
            }
        }
    }
}

data class OwnershipContinuation(
    val afterDate: Instant
): Continuation {

    constructor(afterDateStr: String): this(Instant.ofEpochSecond(afterDateStr.toLong()))
}
