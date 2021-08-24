package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.OwnershipId
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

data class NftCollectionContinuation(
    val afterDate: Instant,
    val afterId: String
) : Continuation {
    override fun toString(): String {
        return "${afterDate.epochSecond}$SEPARATOR$afterId"
    }

    companion object {
        const val SEPARATOR = '_'

        fun parse(str: String?): NftCollectionContinuation? {
            return if (str == null || str.isEmpty()) {
                null
            } else {

                if (str.contains(SEPARATOR)) {
                    val (dateStr, idStr) = str.split(SEPARATOR)
                    NftCollectionContinuation(Instant.ofEpochSecond(dateStr.toLong()), idStr)
                } else {
                    null
                }
            }
        }
    }
}

data class OwnershipContinuation(
    val beforeDate: Instant,
    val beforeId: OwnershipId
) : Continuation {

    override fun toString(): String {
        return "${beforeDate.epochSecond}_${beforeId}"
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
                    beforeDate = Instant.ofEpochSecond(dateStr.toLong()),
                    beforeId = OwnershipId.parse(ownershipId)
                )
            } else {
                null
            }
        }
    }
}

data class ActivityContinuation(val beforeDate: Instant, val beforeId: String) {

    override fun toString(): String = "${beforeDate.epochSecond}_$beforeId"

    companion object {

        fun of(continuation: String?): ActivityContinuation? {
            if (continuation.isNullOrEmpty()) {
                return null
            }

            return if (continuation.contains("_")) {
                val (dateStr, activityId) = continuation.split("_")
                ActivityContinuation(beforeDate = Instant.ofEpochSecond(dateStr.toLong()), beforeId = activityId)
            } else {
                null
            }
        }
    }
}
