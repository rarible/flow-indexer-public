package com.rarible.flow.core.repository

import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.OwnershipId
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lt
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.regex.Pattern
import kotlin.reflect.KProperty


const val DEFAULT_LIMIT: Int = 50

//todo try to get rid
data class ActivityContinuation(val beforeDate: Instant, val beforeId: String) {

    override fun toString(): String = "${beforeDate.toEpochMilli()}_$beforeId"

    companion object {

        fun of(continuation: String?): ActivityContinuation? {
            if (continuation.isNullOrEmpty()) {
                return null
            }

            return if (continuation.contains("_")) {
                val (dateStr, activityId) = continuation.split("_")
                ActivityContinuation(beforeDate = Instant.ofEpochMilli(dateStr.toLong()), beforeId = activityId)
            } else {
                null
            }
        }
    }
}

sealed class Cont<P1, P2>(open val primary: P1, open val secondary: P2) {
    abstract operator fun invoke(
        criteria: Criteria,
        primaryProp: KProperty<P1>,
        secondaryProp: KProperty<P2>
    ): Criteria

    data class AscCont<P1, P2>(override val primary: P1, override val secondary: P2) :
        Cont<P1, P2>(primary, secondary) {
        override fun invoke(criteria: Criteria, primaryProp: KProperty<P1>, secondaryProp: KProperty<P2>): Criteria {
            return criteria.orOperator(
                primaryProp gt primary,
                Criteria().andOperator(
                    primaryProp isEqualTo primary,
                    secondaryProp gt secondary
                )
            )
        }
    }

    data class DescCont<P1, P2>(override val primary: P1, override val secondary: P2) :
        Cont<P1, P2>(primary, secondary) {
        override fun invoke(criteria: Criteria, primaryProp: KProperty<P1>, secondaryProp: KProperty<P2>): Criteria {
            return criteria.orOperator(
                primaryProp lt primary,
                Criteria().andOperator(
                    primaryProp isEqualTo primary,
                    secondaryProp lt secondary
                )
            )
        }
    }

    companion object {
        val SPLITTER = Pattern.compile("_")

        inline fun <reified P> parseField(str: String): P {
            return when (P::class) {
                String::class -> str
                Int::class -> str.toInt()
                Long::class -> str.toLong()
                Instant::class -> Instant.ofEpochMilli(str.toLong())
                LocalDateTime::class -> LocalDateTime.ofInstant(Instant.ofEpochMilli(str.toLong()), ZoneOffset.UTC)
                BigDecimal::class -> str.toBigDecimal()
                BigInteger::class -> str.toBigInteger()
                ItemId::class -> ItemId.parse(str)
                OwnershipId::class -> OwnershipId.parse(str)

                else -> throw IllegalArgumentException("Not supported field type in continuation part [$str]")
            } as P
        }

        inline fun <reified P1, reified P2> asc(str: String): Cont<P1, P2> {
            val (f, s) = str.split('_', limit = 2)
            return AscCont(
                parseField(f),
                parseField(s)
            )
        }

        inline fun <reified P1, reified P2> desc(str: String): Cont<P1, P2> {
            val (f, s) = str.split('_', limit = 2)
            return DescCont(
                parseField(f),
                parseField(s)
            )
        }

        inline fun <reified P1, reified P2> scrollAsc(
            criteria: Criteria, continuation: String?,
            primary: KProperty<P1>, secondary: KProperty<P2>
        ): Criteria {
            return if (continuation == null) {
                criteria
            } else {
                asc<P1, P2>(continuation)(criteria, primary, secondary)
            }
        }

        inline fun <reified P1, reified P2> scrollDesc(
            criteria: Criteria, continuation: String?,
            primary: KProperty<P1>, secondary: KProperty<P2>
        ): Criteria {
            return if (continuation == null) {
                criteria
            } else {
                desc<P1, P2>(continuation)(criteria, primary, secondary)
            }
        }

        inline fun <reified P1, reified P2> toString(
            primary: P1, secondary: P2
        ): String {
            return toString(primary) + "_" + toString(secondary)
        }

        inline fun <reified P> toString(property: P): String {
            return when (P::class) {
                Instant::class -> (property as Instant).toEpochMilli().toString()
                LocalDateTime::class -> (property as LocalDateTime).toInstant(ZoneOffset.UTC).toEpochMilli().toString()
                else -> property.toString()
            }
        }
    }
}
