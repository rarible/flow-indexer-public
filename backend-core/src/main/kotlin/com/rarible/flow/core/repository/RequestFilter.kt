package com.rarible.flow.core.repository

import org.springframework.data.mongodb.core.query.Query


interface RequestFilter {
    fun toQuery(): Query
}