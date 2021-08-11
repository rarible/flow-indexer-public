package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.OwnershipId
import com.rarible.flow.core.domain.TokenId
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

sealed interface Continuation

data class NftItemContinuation(
    val afterDate: Instant,
    val afterId: ItemId
) : Continuation {
    override fun toString(): String {
        return "${afterDate.epochSecond}$SEPARATOR$afterId"
    }

    companion object {
        const val SEPARATOR = '_'

        fun parse(str: String?): NftItemContinuation? {
            return if (str == null || str.isEmpty()) {
                null
            } else {

                if (str.contains(SEPARATOR)) {
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
    val afterDate: Instant,
    val afterId: OwnershipId
) : Continuation {

    override fun toString(): String {
        return "${afterDate.epochSecond}_${afterId}"
    }

    companion object {
        /**
         * Create continuation from string, like "Instant.epochSeconds_OwnershipId"
         *
         */
        fun of(continuation: String?): OwnershipContinuation? {
            if (continuation.isNullOrEmpty()) {
                return null
            }

            return if (continuation.contains("_")) {
                val (dateStr, ownershipId) = continuation.split("_")
                OwnershipContinuation(
                    afterDate = Instant.ofEpochSecond(dateStr.toLong()),
                    afterId = OwnershipId.parse(ownershipId)
                )
            } else {
                null
            }
        }
    }
}

data class ActivityContinuation(val afterDate: Instant) {

    override fun toString(): String = "${afterDate.epochSecond}"

    companion object {

        fun of(continuation: String?): ActivityContinuation? =
            if (continuation.isNullOrEmpty()) null else ActivityContinuation(Instant.ofEpochSecond(continuation.toLong()))
    }
}

data class OrderContinuation(val afterDate: LocalDateTime, val afterId: Long) {

    override fun toString(): String = "${afterDate.toEpochSecond(ZoneOffset.UTC)}_$afterId"

    companion object {

        fun of(continuation: String?): OrderContinuation? {
            if (continuation.isNullOrEmpty()) return null

            if (!continuation.contains("_")) return null

            val (afterDateStr, afterIdStr) = continuation.split("_")
            return OrderContinuation(
                afterDate = LocalDateTime.ofEpochSecond(afterDateStr.toLong(), 0, ZoneOffset.UTC),
                afterId = afterIdStr.toLong()
            )
        }
    }
}
