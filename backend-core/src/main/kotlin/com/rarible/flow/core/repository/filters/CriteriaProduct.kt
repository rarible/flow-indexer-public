package com.rarible.flow.core.repository.filters

import org.springframework.data.mongodb.core.query.Criteria

interface CriteriaProduct<T: CriteriaProduct<T>> {

    fun criteria(): Criteria

    fun byCriteria(criteria: Criteria): T

    operator fun times(other: T): T {
        val empty = Criteria()

        @Suppress("ReplaceCallWithBinaryOperator")
        val finalCriteria = if(this.criteria().equals(empty) && other.criteria().equals(empty)) {
            empty
        } else if (this.criteria().equals(empty)) {
            other.criteria()
        } else if (other.criteria().equals(empty)){
            this.criteria()
        } else {
            Criteria().andOperator(
                this.criteria(),
                other.criteria()
            )
        }
        return byCriteria(finalCriteria)
    }
}