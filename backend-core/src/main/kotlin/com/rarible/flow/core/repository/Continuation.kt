package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.ItemId
import java.time.Instant

data class Continuation(
    val afterDate: Instant,
    val afterId: ItemId
) {
    override fun toString(): String {
        return "${afterDate.epochSecond}$SEPARATOR$afterId"
    }

    companion object {
        const val SEPARATOR = '_'

        fun parse(str: String?): Continuation? {
            return if(str == null || str.isEmpty()) {
                null
            } else {

                if(str.contains(SEPARATOR)) {
                    val (dateStr, idStr) = str.split(SEPARATOR)
                    Continuation(Instant.ofEpochSecond(dateStr.toLong()), ItemId.parse(idStr))
                } else {
                    null
                }
            }
        }
    }
}